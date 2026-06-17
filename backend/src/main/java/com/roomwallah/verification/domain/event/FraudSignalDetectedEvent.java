package com.roomwallah.verification.domain.event;

import com.roomwallah.verification.domain.entity.SeverityLevel;

import java.time.Instant;
import java.util.UUID;

public record FraudSignalDetectedEvent(
    UUID userId,
    String signalType,
    SeverityLevel severity,
    int brokerRiskScore,
    String correlationId,
    Instant timestamp
) {}
