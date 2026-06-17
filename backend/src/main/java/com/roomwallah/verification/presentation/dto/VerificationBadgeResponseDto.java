package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class VerificationBadgeResponseDto {
    String badgeLevel;
    Instant awardedAt;
    Instant expiresAt;
}
