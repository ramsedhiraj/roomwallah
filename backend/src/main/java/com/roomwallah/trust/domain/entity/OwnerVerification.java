package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owner_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerVerification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_level", nullable = false, length = 50)
    private VerificationLevel verificationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_provider", nullable = false, length = 50)
    private VerificationProvider verificationProvider;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "idempotency_key")
    private String idempotencyKey;
}
