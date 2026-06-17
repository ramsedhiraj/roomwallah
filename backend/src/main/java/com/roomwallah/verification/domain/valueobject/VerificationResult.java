package com.roomwallah.verification.domain.valueobject;

import java.math.BigDecimal;

public record VerificationResult(
    boolean success,
    String verifiedName,
    String providerReference,
    BigDecimal confidenceScore,
    String failureReason
) {
    public static VerificationResult success(String verifiedName, String providerReference, BigDecimal confidenceScore) {
        return new VerificationResult(true, verifiedName, providerReference, confidenceScore, null);
    }

    public static VerificationResult failure(String failureReason) {
        return new VerificationResult(false, null, null, null, failureReason);
    }
}
