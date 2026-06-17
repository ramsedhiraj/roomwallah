package com.roomwallah.verification.application.listener;

import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.verification.application.service.TrustScoreService;
import com.roomwallah.verification.domain.entity.VerificationAudit;
import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.entity.VerificationRequestStatus;
import com.roomwallah.verification.domain.event.*;
import com.roomwallah.verification.domain.repository.VerificationAuditRepository;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationEventListener {

    private final TrustScoreService trustScoreService;
    private final VerificationRequestRepository requestRepository;
    private final VerificationAuditRepository auditRepository;
    private final Clock clock;

    @Async
    @EventListener
    @Transactional
    public void handleVerificationApproved(VerificationApprovedEvent event) {
        log.info("Async Listener: Received VerificationApprovedEvent for user: {}, requestId: {}, correlationId: {}", 
                event.userId(), event.verificationRequestId(), event.correlationId());
        
        CorrelationContext.set(event.correlationId());
        try {
            // 1. Fetch Request
            VerificationRequest request = requestRepository.findById(event.verificationRequestId()).orElse(null);
            if (request != null && request.getRequestStatus() == VerificationRequestStatus.APPROVED) {
                // 2. Transition request to VERIFIED
                request.transitionTo(VerificationRequestStatus.VERIFIED);
                requestRepository.save(request);

                // Write audit entry
                VerificationAudit audit = new VerificationAudit();
                audit.setVerificationId(request.getId());
                audit.setAction("VERIFIED");
                audit.setActor(event.userId());
                audit.setTimestamp(Instant.now(clock));
                audit.setMetadata("Verification status promoted to VERIFIED after successful downstream processing.");
                auditRepository.save(audit);
                
                log.info("Request {} successfully promoted to VERIFIED status.", request.getId());
            }

            // 3. Recalculate trust scores & badges
            trustScoreService.calculateAndSave(event.userId());
            log.info("Completed trust score recalculation for user: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process verification approval asynchronously: {}", e.getMessage(), e);
        } finally {
            CorrelationContext.clear();
        }
    }

    @Async
    @EventListener
    @Transactional
    public void handleVerificationExpired(VerificationExpiredEvent event) {
        log.info("Async Listener: Received VerificationExpiredEvent for user: {}, correlationId: {}", event.userId(), event.correlationId());
        CorrelationContext.set(event.correlationId());
        try {
            // Recalculate trust scores & badges
            trustScoreService.calculateAndSave(event.userId());
            log.info("Completed trust score update after expiration for user: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process verification expiration asynchronously: {}", e.getMessage(), e);
        } finally {
            CorrelationContext.clear();
        }
    }

    @Async
    @EventListener
    @Transactional
    public void handleVerificationRejected(VerificationRejectedEvent event) {
        log.info("Async Listener: Received VerificationRejectedEvent for user: {}, correlationId: {}", event.userId(), event.correlationId());
        CorrelationContext.set(event.correlationId());
        try {
            // Recalculate trust scores & badges
            trustScoreService.calculateAndSave(event.userId());
            log.info("Completed trust score update after rejection for user: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process verification rejection asynchronously: {}", e.getMessage(), e);
        } finally {
            CorrelationContext.clear();
        }
    }

    @Async
    @EventListener
    @Transactional
    public void handleFraudSignalDetected(FraudSignalDetectedEvent event) {
        log.info("Async Listener: Received FraudSignalDetectedEvent for user: {}, severity: {}, correlationId: {}", 
                event.userId(), event.severity(), event.correlationId());
        CorrelationContext.set(event.correlationId());
        try {
            // Recalculate trust scores & badges
            trustScoreService.calculateAndSave(event.userId());
            log.info("Completed trust score update after fraud signal detection for user: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process fraud signal asynchronously: {}", e.getMessage(), e);
        } finally {
            CorrelationContext.clear();
        }
    }
}
