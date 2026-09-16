package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.identity.presentation.dto.UserProfileResponse;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshSessionService refreshSessionService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final com.roomwallah.verification.application.service.OtpService otpService;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

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

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void forgotPassword(String email) {
        passwordResetService.forgotPassword(email);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        passwordResetService.resetPassword(email, code, newPassword, confirmPassword);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));
        boolean success = otpService.verifyOtp(user.getId(), code, "EMAIL_OTP");
        if (!success) {
            throw new IllegalArgumentException("Invalid or expired email verification code");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void verifyEmailByToken(String token) {
        emailVerificationService.verifyEmailToken(token);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void resendVerification(String email) {
        emailVerificationService.resendVerificationEmail(email);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void requestLoginOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));
        if (user.isDeleted()) {
            throw new IllegalArgumentException("Account has been deleted");
        }
        if (user.getStatus() == com.roomwallah.user.entity.AccountStatus.DISABLED) {
            throw new IllegalArgumentException("Account is disabled");
        }
        if (user.getStatus() == com.roomwallah.user.entity.AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }
        otpService.generateOtp(user.getId(), email, "LOGIN_OTP");
    }
}
