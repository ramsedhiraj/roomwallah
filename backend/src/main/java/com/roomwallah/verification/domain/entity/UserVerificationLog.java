package com.roomwallah.verification.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_verification_logs")
@Getter
@Setter
public class UserVerificationLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "verification_type", nullable = false, length = 50)
    private String verificationType; // EMAIL_OTP, MOBILE_OTP

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, VERIFIED, EXPIRED, FAILED

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
