package com.roomwallah.trust.application.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.entity.VerificationStatus;
import com.roomwallah.trust.domain.event.VerificationApprovedEvent;
import com.roomwallah.trust.domain.event.VerificationRejectedEvent;
import com.roomwallah.trust.domain.port.OwnerVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
public class VerificationApprovalService {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TrustScoreService trustScoreService;
    private final NotificationPort notificationPort;
    private final AuditPort auditPort;

    public VerificationApprovalService(
            OwnerVerificationRepository ownerVerificationRepository,
            OutboxEventPublisher outboxEventPublisher,
            TrustScoreService trustScoreService,
            NotificationPort notificationPort,
            AuditPort auditPort) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.trustScoreService = trustScoreService;
        this.notificationPort = notificationPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public OwnerVerification approveVerification(UUID verificationId, UUID reviewerId) {
        log.info("Approving verification: {} by reviewer: {}", verificationId, reviewerId);
        OwnerVerification verification = ownerVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found: " + verificationId));

        verification.setVerificationStatus(VerificationStatus.APPROVED);
        verification.setApprovedAt(Instant.now());
        verification.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(365)));
        verification.setReviewerId(reviewerId);
        ownerVerificationRepository.save(verification);

        // Audit log
        var details = new HashMap<String, Object>();
        details.put("verificationId", verificationId);
        details.put("reviewerId", reviewerId);
        auditPort.log("VERIFICATION_APPROVED", verification.getUserId().toString(), "SYSTEM", details);

        // Notify user
        notificationPort.sendEmail(
                verification.getUserId().toString() + "@example.com",
                "Verification Approved",
                "Congratulations, your owner verification request has been approved!"
        );

        // Publish event
        outboxEventPublisher.persistEvent("OwnerVerification", verification.getId().toString(),
                new VerificationApprovedEvent(verification.getId(), verification.getUserId(), Instant.now()));

        // Recalculate trust score
        trustScoreService.recalculateTrustScore(verification.getUserId(), "VERIFICATION_APPROVED");

        return verification;
    }

    @Transactional
    public OwnerVerification rejectVerification(UUID verificationId, UUID reviewerId, String reason) {
        log.info("Rejecting verification: {} by reviewer: {}, reason: {}", verificationId, reviewerId, reason);
        OwnerVerification verification = ownerVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found: " + verificationId));

        verification.setVerificationStatus(VerificationStatus.REJECTED);
        verification.setRejectedAt(Instant.now());
        verification.setReviewerId(reviewerId);
        verification.setRejectionReason(reason);
        ownerVerificationRepository.save(verification);

        // Audit log
        var details = new HashMap<String, Object>();
        details.put("verificationId", verificationId);
        details.put("reviewerId", reviewerId);
        details.put("reason", reason);
        auditPort.log("VERIFICATION_REJECTED", verification.getUserId().toString(), "SYSTEM", details);

        // Notify user
        notificationPort.sendEmail(
                verification.getUserId().toString() + "@example.com",
                "Verification Rejected",
                "We regret to inform you that your verification request has been rejected. Reason: " + reason
        );

        // Publish event
        outboxEventPublisher.persistEvent("OwnerVerification", verification.getId().toString(),
                new VerificationRejectedEvent(verification.getId(), verification.getUserId(), Instant.now(), reason));

        // Recalculate trust score
        trustScoreService.recalculateTrustScore(verification.getUserId(), "VERIFICATION_REJECTED");

        return verification;
    }
}
