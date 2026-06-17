package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DigiLockerVerificationAdapter implements VerificationProviderPort {

    @Override
    public VerificationResult submitRequest(UUID userId, String code, String correlationId) {
        throw new UnsupportedOperationException("DigiLocker provider integration is not yet implemented.");
    }

    @Override
    public VerificationResult checkStatus(String providerReference, String correlationId) {
        throw new UnsupportedOperationException("DigiLocker status check is not yet implemented.");
    }

    @Override
    public VerificationResult fetchResult(String providerReference, String correlationId) {
        throw new UnsupportedOperationException("DigiLocker result retrieval is not yet implemented.");
    }

    @Override
    public boolean cancelRequest(String providerReference, String correlationId) {
        return false;
    }

    @Override
    public String getProviderMetadata() {
        return "DigiLocker API integration placeholder";
    }

    @Override
    public boolean reportHealth() {
        return false;
    }

    @Override
    public VerificationProvider getProviderType() {
        return VerificationProvider.DIGILOCKER;
    }
}
