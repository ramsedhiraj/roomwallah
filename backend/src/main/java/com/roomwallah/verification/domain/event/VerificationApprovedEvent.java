package com.roomwallah.verification.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationApprovedEvent(
    UUID userId,
    UUID verificationRequestId,
    String correlationId,
    Instant timestamp
) {}
