package com.roomwallah.verification.domain.port;

import java.util.UUID;

public interface IdentityVerificationProvider {
    boolean verifyAadhaar(UUID userId, String aadhaarNumber, boolean consentGiven);
}
