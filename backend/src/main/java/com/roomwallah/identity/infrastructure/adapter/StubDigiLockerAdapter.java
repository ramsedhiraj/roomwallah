package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.ExternalIdentityPort;
import com.roomwallah.identity.domain.port.ExternalUserInfoDto;
import org.springframework.stereotype.Component;

@Component
public class StubDigiLockerAdapter implements ExternalIdentityPort {

    @Override
    public boolean supports(String provider) {
        return "digilocker".equalsIgnoreCase(provider);
    }

    @Override
    public ExternalUserInfoDto fetchUserInfo(String token) {
        return ExternalUserInfoDto.builder()
                .externalId("digilocker-mock-id-345678")
                .email("mock.digilocker.user@example.com")
                .fullName("DigiLocker Mock User")
                .build();
    }
}
