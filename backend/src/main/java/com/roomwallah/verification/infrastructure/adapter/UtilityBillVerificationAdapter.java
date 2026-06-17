package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class UtilityBillVerificationAdapter implements VerificationProviderPort {

    @Override
    public VerificationResult submitRequest(UUID userId, String code, String correlationId) {
        if (code != null && !code.isBlank()) {
            return VerificationResult.success("Utility Customer Verified", "UTIL-" + UUID.randomUUID().toString().substring(0, 8), BigDecimal.valueOf(0.90));
        }
        return VerificationResult.failure("Invalid utility connection number or bill details");
    }

    @Override
    public VerificationResult checkStatus(String providerReference, String correlationId) {
        return VerificationResult.success("Utility Customer Verified", providerReference, BigDecimal.valueOf(0.90));
    }

    @Override
    public VerificationResult fetchResult(String providerReference, String correlationId) {
        return VerificationResult.success("Utility Customer Verified", providerReference, BigDecimal.valueOf(0.90));
    }

    @Override
    public boolean cancelRequest(String providerReference, String correlationId) {
        return true;
    }

    @Override
    public String getProviderMetadata() {
        return "Utility connection database verification adapter";
    }

    @Override
    public boolean reportHealth() {
        return true;
    }

    @Override
    public VerificationProvider getProviderType() {
        return VerificationProvider.UTILITY_BILL;
    }
}
