package com.roomwallah.verification.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import com.roomwallah.exception.InvalidStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_requests")
@Getter
@Setter
public class VerificationRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private VerificationProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 50)
    private VerificationRequestStatus requestStatus = VerificationRequestStatus.PENDING;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "verified_name", length = 255)
    private String verifiedName;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "idempotency_expires_at")
    private Instant idempotencyExpiresAt;

    @Column(name = "idempotency_cleanup_after")
    private Instant idempotencyCleanupAfter;

    @Column(name = "verification_version", nullable = false)
    private int verificationVersion = 1;

    public void transitionTo(VerificationRequestStatus newStatus) {
        if (this.requestStatus == null) {
            this.requestStatus = VerificationRequestStatus.PENDING;
        }

        boolean valid = false;
        switch (this.requestStatus) {
            case PENDING:
                valid = (newStatus == VerificationRequestStatus.UNDER_REVIEW 
                      || newStatus == VerificationRequestStatus.REJECTED);
                break;
            case UNDER_REVIEW:
                valid = (newStatus == VerificationRequestStatus.APPROVED 
                      || newStatus == VerificationRequestStatus.REJECTED);
                break;
            case APPROVED:
                valid = (newStatus == VerificationRequestStatus.VERIFIED 
                      || newStatus == VerificationRequestStatus.REJECTED);
                break;
            case VERIFIED:
                valid = (newStatus == VerificationRequestStatus.EXPIRED 
                      || newStatus == VerificationRequestStatus.REJECTED);
                break;
            case REJECTED:
            case EXPIRED:
                valid = (newStatus == VerificationRequestStatus.UNDER_REVIEW);
                break;
            default:
                break;
        }

        if (!valid) {
            throw new InvalidStateTransitionException(
                "Cannot transition verification request from " + this.requestStatus + " to " + newStatus
            );
        }
        this.requestStatus = newStatus;
    }
}
