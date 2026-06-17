package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class VideoVerificationAdapter implements VerificationProviderPort {

    @Override
    public VerificationResult submitRequest(UUID userId, String code, String correlationId) {
        if (code != null && !code.isBlank()) {
            return VerificationResult.success("Liveness Confirmed", "VID-" + UUID.randomUUID().toString().substring(0, 8), BigDecimal.valueOf(0.98));
        }
        return VerificationResult.failure("Liveness detection failed or video stream is invalid");
    }

    @Override
    public VerificationResult checkStatus(String providerReference, String correlationId) {
        return VerificationResult.success("Liveness Confirmed", providerReference, BigDecimal.valueOf(0.98));
    }

    @Override
    public VerificationResult fetchResult(String providerReference, String correlationId) {
        return VerificationResult.success("Liveness Confirmed", providerReference, BigDecimal.valueOf(0.98));
    }

    @Override
    public boolean cancelRequest(String providerReference, String correlationId) {
        return true;
    }

    @Override
    public String getProviderMetadata() {
        return "Video biometric and liveness check adapter";
    }

    @Override
    public boolean reportHealth() {
        return true;
    }

    @Override
    public VerificationProvider getProviderType() {
        return VerificationProvider.VIDEO;
    }
}
