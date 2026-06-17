package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FraudSignalResponseDto {
    UUID id;
    UUID userId;
    String signalType;
    String severity;
    int brokerRiskScore;
    String description;
    Instant createdAt;
}
