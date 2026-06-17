package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.domain.port.AuthenticationProviderStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationProviderFactory {

    private final List<AuthenticationProviderStrategy> strategies;

    public AuthenticationProviderStrategy getStrategy(AuthType type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Authentication method not supported: " + type));
    }
}
