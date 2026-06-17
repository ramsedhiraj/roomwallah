package com.roomwallah.verification.domain.event;

import com.roomwallah.verification.domain.entity.BadgeLevel;

import java.time.Instant;
import java.util.UUID;

public record BadgeAwardedEvent(
    UUID userId,
    BadgeLevel badgeLevel,
    String correlationId,
    Instant timestamp
) {}
