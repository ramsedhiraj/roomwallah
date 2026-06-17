package com.roomwallah.verification.infrastructure.adapter;

import com.roomwallah.exception.ProviderCommunicationException;
import com.roomwallah.exception.VerificationFailedException;
import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import com.roomwallah.verification.domain.valueobject.VerificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class StubVerificationProviderAdapter implements VerificationProviderPort {

    @Override
    public VerificationResult submitRequest(UUID userId, String code, String correlationId) {
        log.info("Stub Provider: Submitting verification for user: {}, code: {}, correlation ID: {}", userId, code, correlationId);
        
        if ("FAIL_TIMEOUT".equalsIgnoreCase(code)) {
            log.error("Stub Provider: Simulating timeout failure");
            throw new ProviderCommunicationException("Provider request timed out (simulated)");
        }
        if ("FAIL_COMMUNICATION".equalsIgnoreCase(code)) {
            log.error("Stub Provider: Simulating communication connection failure");
            throw new ProviderCommunicationException("Connection failed (simulated)");
        }
        if ("FAIL_INVALID".equalsIgnoreCase(code)) {
            log.warn("Stub Provider: Simulating invalid credentials failure");
            return VerificationResult.failure("Invalid mock verification details");
        }
        if ("FAIL_BUSINESS".equalsIgnoreCase(code)) {
            log.warn("Stub Provider: Simulating generic business exception");
            throw new VerificationFailedException("Mock verification rejected by business rules");
        }

        // Default successful mock response
        String providerReference = "STUB-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return VerificationResult.success(
            "Mock Verified User", 
            providerReference, 
            BigDecimal.valueOf(99.00)
        );
    }

    @Override
    public VerificationResult checkStatus(String providerReference, String correlationId) {
        log.info("Stub Provider: Checking status for reference: {}, correlation: {}", providerReference, correlationId);
        return VerificationResult.success("Mock Verified User", providerReference, BigDecimal.valueOf(99.00));
    }

    @Override
    public VerificationResult fetchResult(String providerReference, String correlationId) {
        log.info("Stub Provider: Fetching result for reference: {}, correlation: {}", providerReference, correlationId);
        return VerificationResult.success("Mock Verified User", providerReference, BigDecimal.valueOf(99.00));
    }

    @Override
    public boolean cancelRequest(String providerReference, String correlationId) {
        log.info("Stub Provider: Canceling request for reference: {}, correlation: {}", providerReference, correlationId);
        return true;
    }

    @Override
    public String getProviderMetadata() {
        return "Stub Mock Verification Provider v1.0";
    }

    @Override
    public boolean reportHealth() {
        return true;
    }

    @Override
    public VerificationProvider getProviderType() {
        return VerificationProvider.STUB;
    }
}
