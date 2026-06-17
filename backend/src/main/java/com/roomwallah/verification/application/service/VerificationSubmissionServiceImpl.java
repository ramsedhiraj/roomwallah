package com.roomwallah.verification.application.service;

import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.exception.ProviderCommunicationException;
import com.roomwallah.exception.VerificationFailedException;
import com.roomwallah.verification.domain.entity.*;
import com.roomwallah.verification.domain.port.EventPublisherPort;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.repository.VerificationAuditRepository;
import com.roomwallah.verification.domain.repository.VerificationProviderMetricsRepository;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import com.roomwallah.verification.domain.event.VerificationSubmittedEvent;
import com.roomwallah.verification.domain.event.VerificationApprovedEvent;
import com.roomwallah.verification.domain.event.VerificationRejectedEvent;
import com.roomwallah.verification.infrastructure.provider.VerificationProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationSubmissionServiceImpl implements VerificationSubmissionService {

    private final VerificationRequestRepository requestRepository;
    private final VerificationAuditRepository auditRepository;
    private final VerificationProviderMetricsRepository metricsRepository;
    private final VerificationProviderRegistry providerRegistry;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public VerificationRequest submit(UUID userId, VerificationProvider provider, String code, String idempotencyKey) {
        log.info("Starting verification submission for user: {}, provider: {}, key: {}", userId, provider, idempotencyKey);

        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<VerificationRequest> existingOpt = requestRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                log.info("Found existing verification request with idempotency key: {}", idempotencyKey);
                return existingOpt.get();
            }
        }

        // 2. Resolve versioning
        Optional<VerificationRequest> highestVersionOpt = requestRepository.findFirstByUserIdOrderByVerificationVersionDesc(userId);
        int newVersion = highestVersionOpt.map(r -> r.getVerificationVersion() + 1).orElse(1);

        // 3. Create Verification Request
        Instant now = Instant.now(clock);
        VerificationRequest request = new VerificationRequest();
        request.setUserId(userId);
        request.setProvider(provider);
        request.setRequestStatus(VerificationRequestStatus.PENDING);
        request.setSubmittedAt(now);
        request.setIdempotencyKey(idempotencyKey);
        request.setVerificationVersion(newVersion);
        
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            // Set default idempotency expiration to 24h, cleanup after 48h
            request.setIdempotencyExpiresAt(now.plus(Duration.ofHours(24)));
            request.setIdempotencyCleanupAfter(now.plus(Duration.ofHours(48)));
        }

        // Set default verification expiry to 365 days
        request.setExpiresAt(now.plus(Duration.ofDays(365)));

        request = requestRepository.save(request);

        // 4. Log Immutable Audit record
        logAudit(request.getId(), "SUBMITTED", userId, "Verification request submitted with version: " + newVersion);

        // Publish Submitted Event
        eventPublisher.publish(new VerificationSubmittedEvent(
            userId,
            request.getId(),
            CorrelationContext.get(),
            now
        ));

        // 5. Execute Provider validation
        long startTime = System.currentTimeMillis();
        boolean isFailed = false;
        boolean isTimeout = false;
        VerificationResult result = null;

        try {
            // Transition state to UNDER_REVIEW
            request.transitionTo(VerificationRequestStatus.UNDER_REVIEW);
            request = requestRepository.save(request);
            logAudit(request.getId(), "MOVED_TO_REVIEW", userId, "Verification request moved to under review status");

            VerificationProviderPort providerPort = providerRegistry.getProvider(provider);
            result = providerPort.submitRequest(userId, code, CorrelationContext.get());
        } catch (ProviderCommunicationException pce) {
            isFailed = true;
            if (pce.getMessage().contains("timeout") || pce.getMessage().contains("timed out")) {
                isTimeout = true;
            }
            throw pce;
        } catch (Exception e) {
            isFailed = true;
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            updateProviderMetrics(provider, latency, isFailed, isTimeout);
        }

        // 6. Handle Provider validation outcome
        if (result != null && result.success()) {
            request.setVerifiedName(result.verifiedName());
            request.setProviderReference(result.providerReference());
            request.setConfidenceScore(result.confidenceScore() != null ? result.confidenceScore() : BigDecimal.valueOf(100.00));
            request.setCompletedAt(Instant.now(clock));

            // Instant provider approval (e.g. for Stub provider)
            request.transitionTo(VerificationRequestStatus.APPROVED);
            request = requestRepository.save(request);

            logAudit(request.getId(), "APPROVED", userId, "Verification approved by provider reference: " + result.providerReference());

            eventPublisher.publish(new VerificationApprovedEvent(
                userId,
                request.getId(),
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        } else if (result != null) {
            request.setRejectionReason(result.failureReason());
            request.setCompletedAt(Instant.now(clock));
            request.setRequestStatus(VerificationRequestStatus.REJECTED);
            request = requestRepository.save(request);

            logAudit(request.getId(), "REJECTED", userId, "Verification rejected by provider: " + result.failureReason());

            eventPublisher.publish(new VerificationRejectedEvent(
                userId,
                request.getId(),
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        }

        return request;
    }

    @Override
    public VerificationRequest getActiveVerification(UUID userId) {
        return requestRepository.findFirstByUserIdAndRequestStatusOrderByCreatedAtDesc(userId, VerificationRequestStatus.VERIFIED)
            .or(() -> requestRepository.findFirstByUserIdAndRequestStatusOrderByCreatedAtDesc(userId, VerificationRequestStatus.APPROVED))
            .or(() -> requestRepository.findFirstByUserIdAndRequestStatusOrderByCreatedAtDesc(userId, VerificationRequestStatus.UNDER_REVIEW))
            .orElseGet(() -> requestRepository.findFirstByUserIdOrderByVerificationVersionDesc(userId).orElse(null));
    }

    private void logAudit(UUID requestUuid, String action, UUID actor, String metadata) {
        VerificationAudit audit = new VerificationAudit();
        audit.setVerificationId(requestUuid);
        audit.setAction(action);
        audit.setActor(actor);
        audit.setTimestamp(Instant.now(clock));
        audit.setMetadata(metadata);
        auditRepository.save(audit);
    }

    private void updateProviderMetrics(VerificationProvider provider, long latency, boolean isFailed, boolean isTimeout) {
        try {
            VerificationProviderMetrics metrics = metricsRepository.findById(provider.name())
                .orElseGet(() -> {
                    VerificationProviderMetrics newMetrics = new VerificationProviderMetrics();
                    newMetrics.setProvider(provider.name());
                    newMetrics.setLastUpdated(Instant.now(clock));
                    return newMetrics;
                });

            metrics.setTotalRequests(metrics.getTotalRequests() + 1);
            if (isFailed) {
                metrics.setFailedRequests(metrics.getFailedRequests() + 1);
                if (isTimeout) {
                    metrics.setTimeoutRequests(metrics.getTimeoutRequests() + 1);
                }
            } else {
                metrics.setSuccessfulRequests(metrics.getSuccessfulRequests() + 1);
            }

            long total = metrics.getTotalRequests();
            long oldAvg = metrics.getAverageLatencyMs();
            long newAvg = ((oldAvg * (total - 1)) + latency) / total;
            metrics.setAverageLatencyMs(newAvg);
            metrics.setLastUpdated(Instant.now(clock));
            metricsRepository.save(metrics);
        } catch (Exception e) {
            log.error("Failed to update provider metrics: {}", e.getMessage());
        }
    }
}
