package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.identity.presentation.dto.UserProfileResponse;
import com.roomwallah.user.entity.User;

public interface IdentityFacade {
    User register(RegisterRequest request);
    AuthResponse login(String identity, String password, AuthType authType, String deviceName, String browser, String os, String ipAddress);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
    UserProfileResponse getProfile();
    void forgotPassword(String email);
    void resetPassword(String email, String code, String newPassword, String confirmPassword);
    void verifyEmail(String email, String code);
    void requestLoginOtp(String email);
}
