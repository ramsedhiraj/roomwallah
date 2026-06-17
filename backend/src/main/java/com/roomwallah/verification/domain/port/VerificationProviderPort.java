package com.roomwallah.verification.domain.port;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.valueobject.VerificationResult;

import java.util.UUID;

public interface VerificationProviderPort {
    VerificationResult submitRequest(UUID userId, String code, String correlationId);
    VerificationResult checkStatus(String providerReference, String correlationId);
    VerificationResult fetchResult(String providerReference, String correlationId);
    boolean cancelRequest(String providerReference, String correlationId);
    String getProviderMetadata();
    boolean reportHealth();
    VerificationProvider getProviderType();
}
