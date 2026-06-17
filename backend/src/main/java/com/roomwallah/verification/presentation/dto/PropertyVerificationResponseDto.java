package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PropertyVerificationResponseDto {
    private UUID id;
    private UUID propertyId;
    private UUID ownerId;
    private String documentUrl;
    private String utilityBillUrl;
    private boolean deedNameMatched;
    private boolean utilityNameMatched;
    private boolean locationMatched;
    private BigDecimal confidenceScore;
    private String approvalStatus;
    private String rejectionReason;
    private Instant verifiedAt;
}
