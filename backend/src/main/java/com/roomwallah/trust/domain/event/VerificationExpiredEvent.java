package com.roomwallah.trust.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationExpiredEvent(UUID verificationId, UUID userId, Instant expiredAt) {}
