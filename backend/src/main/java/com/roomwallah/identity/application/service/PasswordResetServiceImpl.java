package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.identity.infrastructure.email.EmailTemplates;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final NotificationPort notificationPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final Clock clock;

    @Value("${roomwallah.security.password-reset-expiration-minutes:15}")
    private long resetExpirationMinutes;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
            log.info("Password reset requested for non-existent or deleted user: {}", normalizedEmail);
            // Generic return prevents account enumeration
            return;
        }

        User user = userOpt.get();

        // 60-second cooldown check to prevent abuse
        if (user.getPasswordResetTokenExpiresAt() != null) {
            Instant earliestAllowed = user.getPasswordResetTokenExpiresAt()
                    .minus(resetExpirationMinutes, ChronoUnit.MINUTES)
                    .plus(60, ChronoUnit.SECONDS);
            if (Instant.now(clock).isBefore(earliestAllowed)) {
                log.warn("Password reset requested within cooldown period for: {}", normalizedEmail);
                throw new IllegalStateException("Please wait 60 seconds before requesting another reset code.");
            }
        }

        String resetCode = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = hashToken(resetCode);

        Instant expiresAt = Instant.now(clock).plus(resetExpirationMinutes, ChronoUnit.MINUTES);
        user.setPasswordResetTokenHash(codeHash);
        user.setPasswordResetTokenExpiresAt(expiresAt);
        userRepository.save(user);

        String subject = "Reset Your RoomWallah Password";
        String htmlBody = EmailTemplates.buildPasswordResetHtml(user.getFullName(), resetCode, resetExpirationMinutes);
        String plainTextBody = EmailTemplates.buildPasswordResetText(user.getFullName(), resetCode, resetExpirationMinutes);

        try {
            notificationPort.sendEmail(user.getEmail(), subject, plainTextBody, htmlBody);
            log.info("Password reset email dispatched to: {}", user.getEmail());
            log.info("\n>>> [LOCAL PASSWORD RESET CODE for {}]: {}\n", user.getEmail(), resetCode);
        } catch (Exception e) {
            log.error("Failed to deliver password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset code is required");
        }
        if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long and contain at least " +
                    "one uppercase letter, one lowercase letter, one digit, and one special character."
            );
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset code"));

        if (user.getPasswordResetTokenHash() == null || user.getPasswordResetTokenExpiresAt() == null) {
            log.warn("Password reset attempted with no active token for: {}", normalizedEmail);
            throw new IllegalArgumentException("Invalid or expired password reset code");
        }

        Instant now = Instant.now(clock);
        if (user.getPasswordResetTokenExpiresAt().isBefore(now)) {
            log.warn("Password reset attempted with expired token for: {}", normalizedEmail);
            throw new IllegalArgumentException("Password reset code has expired. Please request a new one.");
        }

        String inputHash = hashToken(code.trim());
        if (!inputHash.equals(user.getPasswordResetTokenHash())) {
            log.warn("Invalid password reset code submitted for: {}", normalizedEmail);
            throw new IllegalArgumentException("Invalid or expired password reset code");
        }

        // Successfully verified reset code - update password and invalidate token
        user.setPasswordHash(passwordEncoderPort.encode(newPassword));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
        user.setFailedLoginCount(0);
        user.setStatus(AccountStatus.ACTIVE);
        user.setLockUntil(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", normalizedEmail);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
