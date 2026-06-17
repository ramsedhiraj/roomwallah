package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.port.IdentityVerificationPort;
import com.roomwallah.trust.domain.valueobject.VerificationMetadata;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class StubIdentityVerificationAdapter implements IdentityVerificationPort {
    @Override
    public VerificationMetadata verifyIdentity(UUID userId, OwnerVerification verification) {
        Map<String, Object> rawDetails = new HashMap<>();
        rawDetails.put("provider", verification.getVerificationProvider().name());
        rawDetails.put("status", "SUCCESS");
        rawDetails.put("confidenceScore", 98.5);
        rawDetails.put("extractedName", "John Owner");
        return new VerificationMetadata(rawDetails);
    }
}
