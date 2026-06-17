package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class VerificationDecisionAuditResponseDto {
    UUID id;
    UUID verificationRequestId;
    UUID adminId;
    String previousStatus;
    String newStatus;
    String decisionReason;
    String correlationId;
    Instant createdAt;
}
