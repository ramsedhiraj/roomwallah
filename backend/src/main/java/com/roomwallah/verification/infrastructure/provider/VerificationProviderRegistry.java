package com.roomwallah.verification.infrastructure.provider;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.port.VerificationProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class VerificationProviderRegistry {

    private final Map<VerificationProvider, VerificationProviderPort> providers = new ConcurrentHashMap<>();
    
    @Value("${roomwallah.verification.default-provider:STUB}")
    private String defaultProviderName;

    public VerificationProviderRegistry(List<VerificationProviderPort> providerList) {
        for (VerificationProviderPort provider : providerList) {
            providers.put(provider.getProviderType(), provider);
            log.info("Registered verification provider: {}", provider.getProviderType());
        }
    }

    public VerificationProviderPort getProvider(VerificationProvider provider) {
        return Optional.ofNullable(providers.get(provider))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported verification provider: " + provider));
    }

    public VerificationProviderPort getDefaultProvider() {
        try {
            VerificationProvider defaultType = VerificationProvider.valueOf(defaultProviderName.toUpperCase().trim());
            return getProvider(defaultType);
        } catch (Exception e) {
            log.error("Invalid default verification provider configured: '{}'. Falling back to STUB.", defaultProviderName);
            return getProvider(VerificationProvider.STUB);
        }
    }
}
