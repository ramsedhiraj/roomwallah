package com.roomwallah.verification.domain.valueobject;

public record TrustBreakdown(
    int identityScore,
    int phoneScore,
    int emailScore,
    int propertyScore,
    int videoScore,
    int reviewScore,
    int activityScore,
    int fraudPenalty
) {}
