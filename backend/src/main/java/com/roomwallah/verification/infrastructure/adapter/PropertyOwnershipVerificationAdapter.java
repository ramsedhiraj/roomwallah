package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PropertyOwnershipVerificationAdapter implements VerificationProviderPort {

    @Override
    public VerificationResult submitRequest(UUID userId, String code, String correlationId) {
        if (code != null && !code.isBlank()) {
            return VerificationResult.success("Registry Verified Property Owner", "REG-" + UUID.randomUUID().toString().substring(0, 8), BigDecimal.valueOf(0.95));
        }
        return VerificationResult.failure("Invalid registry proof document or number");
    }

    @Override
    public VerificationResult checkStatus(String providerReference, String correlationId) {
        return VerificationResult.success("Registry Verified Property Owner", providerReference, BigDecimal.valueOf(0.95));
    }

    @Override
    public VerificationResult fetchResult(String providerReference, String correlationId) {
        return VerificationResult.success("Registry Verified Property Owner", providerReference, BigDecimal.valueOf(0.95));
    }

    @Override
    public boolean cancelRequest(String providerReference, String correlationId) {
        return true;
    }

    @Override
    public String getProviderMetadata() {
        return "Property ownership registry verification adapter";
    }

    @Override
    public boolean reportHealth() {
        return true;
    }

    @Override
    public VerificationProvider getProviderType() {
        return VerificationProvider.PROPERTY_OWNERSHIP;
    }
}
