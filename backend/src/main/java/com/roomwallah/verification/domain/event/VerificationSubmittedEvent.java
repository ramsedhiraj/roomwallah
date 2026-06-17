package com.roomwallah.verification.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationSubmittedEvent(
    UUID userId,
    UUID verificationRequestId,
    String correlationId,
    Instant timestamp
) {}
