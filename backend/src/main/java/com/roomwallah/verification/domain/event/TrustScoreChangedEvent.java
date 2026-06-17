package com.roomwallah.verification.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TrustScoreChangedEvent(
    UUID userId,
    int overallScore,
    String correlationId,
    Instant timestamp
) {}
