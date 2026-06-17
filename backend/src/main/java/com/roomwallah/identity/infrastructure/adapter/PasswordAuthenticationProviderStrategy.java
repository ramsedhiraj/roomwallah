package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.domain.port.AuthenticationProviderStrategy;
import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordAuthenticationProviderStrategy implements AuthenticationProviderStrategy {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    public boolean supports(AuthType type) {
        return AuthType.PASSWORD == type;
    }

    @Override
    public User authenticate(String identity, String credentials) {
        // Find user by email first, then by phone
        Optional<User> userOpt = userRepository.findByEmail(identity);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(identity);
        }

        // Return generic error if not found, soft deleted, or password mismatch
        if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
            log.warn("Login failure - user [{}] not found or deleted.", identity);
            throw new IllegalArgumentException("Invalid email/phone or password");
        }

        User user = userOpt.get();

        // Block login attempts if account is LOCKED, DISABLED, or SUSPENDED
        if (user.getStatus() == AccountStatus.LOCKED) {
            log.warn("Login failure - account is locked for user [{}].", identity);
            throw new IllegalArgumentException("Account is locked");
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            log.warn("Login failure - account is disabled for user [{}].", identity);
            throw new IllegalArgumentException("Account is disabled");
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login failure - account is suspended for user [{}].", identity);
            throw new IllegalArgumentException("Account is suspended");
        }

        if (!passwordEncoderPort.matches(credentials, user.getPasswordHash())) {
            log.warn("Login failure - password mismatch for user [{}].", identity);
            throw new IllegalArgumentException("Invalid email/phone or password");
        }

        return user;
    }
}
