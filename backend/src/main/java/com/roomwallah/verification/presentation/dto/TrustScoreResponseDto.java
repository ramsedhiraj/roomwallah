package com.roomwallah.verification.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TrustScoreResponseDto {
    int overallScore;
    int identityScore;
    int propertyScore;
    int reviewScore;
    int activityScore;
    int fraudPenalty;
    Instant calculatedAt;
}
