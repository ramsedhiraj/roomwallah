package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.ExternalIdentityPort;
import com.roomwallah.identity.domain.port.ExternalUserInfoDto;
import org.springframework.stereotype.Component;

@Component
public class StubAppleAdapter implements ExternalIdentityPort {

    @Override
    public boolean supports(String provider) {
        return "apple".equalsIgnoreCase(provider);
    }

    @Override
    public ExternalUserInfoDto fetchUserInfo(String token) {
        return ExternalUserInfoDto.builder()
                .externalId("apple-mock-id-789012")
                .email("mock.apple.user@example.com")
                .fullName("Apple Mock User")
                .build();
    }
}
