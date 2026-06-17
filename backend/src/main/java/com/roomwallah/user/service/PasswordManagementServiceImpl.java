package com.roomwallah.user.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.event.PasswordChangedEvent;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordManagementServiceImpl implements PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EventPublisherPort eventPublisher;
    private final AuditPort auditPort;

    // Password policy regex: min 8 chars, 1 upper, 1 lower, 1 digit, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        log.info("Processing password change request for user: {}", user.getEmail());

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (!passwordEncoderPort.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        if (!PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new IllegalArgumentException(
                    "New password must be at least 8 characters long and contain at least " +
                    "one uppercase letter, one lowercase letter, one digit, and one special character."
            );
        }

        // Check that new password is not the same as old password
        if (passwordEncoderPort.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoderPort.encode(request.getNewPassword()));
        userRepository.save(user);

        // Publish event
        PasswordChangedEvent event = PasswordChangedEvent.builder()
                .userId(user.getId())
                .changedAt(Instant.now())
                .build();
        eventPublisher.publish(event);

        // Audit log
        auditPort.log(
                "USER_PASSWORD_CHANGE",
                user.getId().toString(),
                "0.0.0.0",
                Map.of(
                        "email", user.getEmail()
                )
        );
    }
}
