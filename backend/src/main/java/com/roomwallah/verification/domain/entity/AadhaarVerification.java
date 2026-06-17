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
@Table(name = "aadhaar_verifications")
@Getter
@Setter
public class AadhaarVerification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "encrypted_aadhaar", nullable = false, columnDefinition = "TEXT")
    private String encryptedAadhaar;

    @Column(name = "masked_aadhaar", nullable = false, length = 20)
    private String maskedAadhaar;

    @Column(name = "consent_tracked", nullable = false)
    private boolean consentTracked = true;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;
}
