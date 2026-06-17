package com.roomwallah.trust.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationRejectedEvent(UUID verificationId, UUID userId, Instant rejectedAt, String reason) {}
