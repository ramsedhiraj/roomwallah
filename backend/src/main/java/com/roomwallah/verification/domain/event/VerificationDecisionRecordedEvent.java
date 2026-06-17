package com.roomwallah.verification.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationDecisionRecordedEvent(
    UUID auditId,
    UUID verificationRequestId,
    UUID adminId,
    String previousStatus,
    String newStatus,
    String correlationId,
    Instant timestamp
) {}
