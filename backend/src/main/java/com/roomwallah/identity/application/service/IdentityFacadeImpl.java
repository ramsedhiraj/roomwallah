package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.identity.presentation.dto.UserProfileResponse;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshSessionService refreshSessionService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public User register(RegisterRequest request) {
        return registrationService.register(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                request.getRole()
        );
    }

    @Override
    public AuthResponse login(String identity, String password, AuthType authType, String deviceName, String browser, String os, String ipAddress) {
        return loginService.login(identity, password, authType, deviceName, browser, os, ipAddress);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        return refreshSessionService.refresh(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        logoutService.logout(refreshToken);
    }

    @Override
    public UserProfileResponse getProfile() {
        User user = currentUserProvider.getCurrentUser();
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }
}
