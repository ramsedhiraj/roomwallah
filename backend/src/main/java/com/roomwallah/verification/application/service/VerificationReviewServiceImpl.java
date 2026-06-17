package com.roomwallah.verification.application.service;

import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.verification.domain.entity.VerificationDecisionAudit;
import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.entity.VerificationRequestStatus;
import com.roomwallah.verification.domain.event.VerificationApprovedEvent;
import com.roomwallah.verification.domain.event.VerificationDecisionRecordedEvent;
import com.roomwallah.verification.domain.event.VerificationExpiredEvent;
import com.roomwallah.verification.domain.event.VerificationRejectedEvent;
import com.roomwallah.verification.domain.port.EventPublisherPort;
import com.roomwallah.verification.domain.repository.VerificationDecisionAuditRepository;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import com.roomwallah.user.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationReviewServiceImpl implements VerificationReviewService {

    private final VerificationRequestRepository requestRepository;
    private final VerificationDecisionAuditRepository decisionAuditRepository;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public VerificationRequest approve(UUID requestId, UUID adminId, String reason) {
        log.info("Admin approving verification request: {}, reason: {}", requestId, reason);
        return handleStateTransition(requestId, adminId, VerificationRequestStatus.APPROVED, reason, "APPROVE");
    }

    @Override
    @Transactional
    public VerificationRequest reject(UUID requestId, UUID adminId, String reason) {
        log.info("Admin rejecting verification request: {}, reason: {}", requestId, reason);
        return handleStateTransition(requestId, adminId, VerificationRequestStatus.REJECTED, reason, "REJECT");
    }

    @Override
    @Transactional
    public VerificationRequest escalate(UUID requestId, UUID adminId, String reason) {
        log.info("Admin escalating verification request: {}, reason: {}", requestId, reason);
        // Escalation keeps request under review but records audit log
        VerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found with ID: " + requestId));
        
        recordDecisionAudit(request, adminId, request.getRequestStatus(), request.getRequestStatus(), reason, "ESCALATE");
        return request;
    }

    @Override
    @Transactional
    public VerificationRequest reopen(UUID requestId, UUID adminId, String reason) {
        log.info("Admin reopening verification request: {}, reason: {}", requestId, reason);
        return handleStateTransition(requestId, adminId, VerificationRequestStatus.UNDER_REVIEW, reason, "REOPEN");
    }

    @Override
    @Transactional
    public VerificationRequest revoke(UUID requestId, UUID adminId, String reason) {
        log.info("Admin revoking verification request: {}, reason: {}", requestId, reason);
        return handleStateTransition(requestId, adminId, VerificationRequestStatus.REJECTED, reason, "REVOKE");
    }

    @Override
    @Transactional
    public VerificationRequest expire(UUID requestId, UUID adminId, String reason) {
        log.info("Admin expiring verification request: {}, reason: {}", requestId, reason);
        return handleStateTransition(requestId, adminId, VerificationRequestStatus.EXPIRED, reason, "EXPIRE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequest> getPendingRequests() {
        return requestRepository.findByRequestStatusOrderByCreatedAtDesc(VerificationRequestStatus.UNDER_REVIEW);
    }

    private VerificationRequest handleStateTransition(UUID requestId, UUID adminId, VerificationRequestStatus newStatus, String reason, String action) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is mandatory for administrative decisions.");
        }

        VerificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found with ID: " + requestId));

        VerificationRequestStatus previousStatus = request.getRequestStatus();
        
        // State Machine validation check
        request.transitionTo(newStatus);
        
        request.setReviewedBy(adminId);
        request.setReviewedAt(Instant.now(clock));
        request.setCompletedAt(Instant.now(clock));
        if (newStatus == VerificationRequestStatus.REJECTED) {
            request.setRejectionReason(reason);
        }
        request = requestRepository.save(request);

        if (newStatus == VerificationRequestStatus.APPROVED) {
            BigDecimal score = request.getConfidenceScore();
            if (score != null && score.compareTo(new BigDecimal("0.85")) >= 0) {
                userRepository.findById(request.getUserId()).ifPresent(user -> {
                    user.setIdentityVerified(true);
                    userRepository.save(user);
                    log.info("Awarded verified badge to user: {}", user.getId());
                });
            }
        }

        // Record Decision Audit
        recordDecisionAudit(request, adminId, previousStatus, newStatus, reason, action);

        // Publish specific events
        if (newStatus == VerificationRequestStatus.APPROVED) {
            eventPublisher.publish(new VerificationApprovedEvent(
                request.getUserId(),
                request.getId(),
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        } else if (newStatus == VerificationRequestStatus.REJECTED) {
            eventPublisher.publish(new VerificationRejectedEvent(
                request.getUserId(),
                request.getId(),
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        } else if (newStatus == VerificationRequestStatus.EXPIRED) {
            eventPublisher.publish(new VerificationExpiredEvent(
                request.getUserId(),
                request.getId(),
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        }

        return request;
    }

    private void recordDecisionAudit(VerificationRequest request, UUID adminId, VerificationRequestStatus previousStatus, VerificationRequestStatus newStatus, String reason, String action) {
        VerificationDecisionAudit audit = new VerificationDecisionAudit();
        audit.setVerificationRequestId(request.getId());
        audit.setAdminId(adminId);
        audit.setPreviousStatus(previousStatus.name());
        audit.setNewStatus(newStatus.name());
        audit.setDecisionReason(reason);
        audit.setCorrelationId(CorrelationContext.get());
        audit.setCreatedAt(Instant.now(clock));

        audit = decisionAuditRepository.save(audit);
        log.info("Persisted immutable administrative decision audit ID: {}", audit.getId());

        // Publish decision recorded event
        eventPublisher.publish(new VerificationDecisionRecordedEvent(
            audit.getId(),
            request.getId(),
            adminId,
            previousStatus.name(),
            newStatus.name(),
            CorrelationContext.get(),
            Instant.now(clock)
        ));
    }
}
