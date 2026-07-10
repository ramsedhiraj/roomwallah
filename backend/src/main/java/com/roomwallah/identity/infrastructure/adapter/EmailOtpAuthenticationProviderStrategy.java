package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.domain.port.AuthenticationProviderStrategy;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOtpAuthenticationProviderStrategy implements AuthenticationProviderStrategy {

    private final UserRepository userRepository;
    private final com.roomwallah.verification.application.service.OtpService otpService;
    private final Clock clock;

    @Override
    public boolean supports(AuthType type) {
        return AuthType.OTP == type;
    }

    @Override
    public User authenticate(String identity, String credentials) {
        if (credentials == null || credentials.isBlank()) {
            throw new IllegalArgumentException("OTP code is required");
        }

        // Find user by email first, then by phone
        Optional<User> userOpt = userRepository.findByEmail(identity);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(identity);
        }

        // Return generic error if not found or soft deleted
        if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
            log.warn("Login failure - user [{}] not found or deleted for OTP login.", identity);
            throw new IllegalArgumentException("Invalid email/phone or OTP");
        }

        User user = userOpt.get();

        // Block login attempts if account is LOCKED, DISABLED, or SUSPENDED
        if (user.getStatus() == AccountStatus.LOCKED) {
            if (user.getLockUntil() != null && user.getLockUntil().isBefore(Instant.now(clock))) {
                user.setStatus(AccountStatus.ACTIVE);
                user.setLockUntil(null);
                user.setFailedLoginCount(0);
                userRepository.save(user);
            } else {
                log.warn("Login failure - account is locked for user [{}].", identity);
                throw new IllegalArgumentException("Account is temporarily locked. Please try again later.");
            }
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            log.warn("Login failure - account is disabled for user [{}].", identity);
            throw new IllegalArgumentException("Account is disabled");
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login failure - account is suspended for user [{}].", identity);
            throw new IllegalArgumentException("Account is suspended");
        }

        boolean success = otpService.verifyOtp(user.getId(), credentials, "LOGIN_OTP");
        if (!success) {
            log.warn("Login failure - invalid OTP for user [{}].", identity);
            throw new IllegalArgumentException("Invalid email/phone or OTP");
        }

        return user;
    }
}
