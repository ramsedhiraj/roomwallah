package com.roomwallah.trust.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TrustScoreChangedEvent(UUID userId, int oldScore, int newScore, Instant calculatedAt) {}
