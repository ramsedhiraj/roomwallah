package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.ExternalIdentityPort;
import com.roomwallah.identity.domain.port.ExternalUserInfoDto;
import org.springframework.stereotype.Component;

@Component
public class StubGoogleAdapter implements ExternalIdentityPort {

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider);
    }

    @Override
    public ExternalUserInfoDto fetchUserInfo(String token) {
        return ExternalUserInfoDto.builder()
                .externalId("google-mock-id-123456")
                .email("mock.google.user@example.com")
                .fullName("Google Mock User")
                .build();
    }
}
