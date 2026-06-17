package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class VerificationRequestResponseDto {
    UUID id;
    UUID userId;
    String provider;
    String requestStatus;
    String verifiedName;
    BigDecimal confidenceScore;
    Instant submittedAt;
    Instant completedAt;
    Instant expiresAt;
    String rejectionReason;
    int verificationVersion;
}
