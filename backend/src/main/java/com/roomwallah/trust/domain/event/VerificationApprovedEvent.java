package com.roomwallah.trust.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VerificationApprovedEvent(UUID verificationId, UUID userId, Instant approvedAt) {}
