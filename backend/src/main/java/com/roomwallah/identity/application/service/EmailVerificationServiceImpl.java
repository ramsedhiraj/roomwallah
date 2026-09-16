package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.NotificationPort;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final NotificationPort notificationPort;
    private final Clock clock;

    @Value("${roomwallah.frontend.url:${APP_FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    @Value("${roomwallah.verification.token-expiration-hours:24}")
    private long tokenExpirationHours;

    @Value("${roomwallah.verification.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void createAndSendVerification(User user) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now(clock).plus(tokenExpirationHours, ChronoUnit.HOURS);
        user.setEmailVerificationTokenHash(tokenHash);
        user.setEmailVerificationTokenExpiresAt(expiresAt);
        userRepository.save(user);

        String trimmedFrontendUrl = frontendUrl.replaceAll("/+$", "");
        String verificationLink = trimmedFrontendUrl + "/verify-email?token=" + rawToken;

        String subject = "Verify your RoomWallah email address";
        String htmlBody = com.roomwallah.identity.infrastructure.email.EmailTemplates
                .buildVerificationHtml(user.getFullName(), verificationLink, tokenExpirationHours);
        String plainTextBody = com.roomwallah.identity.infrastructure.email.EmailTemplates
                .buildVerificationText(user.getFullName(), verificationLink, tokenExpirationHours);

        try {
            notificationPort.sendEmail(user.getEmail(), subject, plainTextBody, htmlBody);
            log.info("Verification email dispatched to: {}", user.getEmail());
            log.info("\n>>> [LOCAL VERIFICATION LINK for {}]: {}\n", user.getEmail(), verificationLink);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            // Do not roll back user creation; allow the user to click resend verification
        }
    }

    @Override
    @Transactional
    public void verifyEmailToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        String tokenHash = hashToken(rawToken.trim());
        Optional<User> userOpt = userRepository.findByEmailVerificationTokenHash(tokenHash);

        if (userOpt.isEmpty()) {
            log.warn("Invalid or previously used verification token submitted.");
            throw new IllegalArgumentException("Verification link is invalid or has already been used.");
        }

        User user = userOpt.get();
        Instant now = Instant.now(clock);

        if (user.getEmailVerificationTokenExpiresAt() == null || user.getEmailVerificationTokenExpiresAt().isBefore(now)) {
            log.warn("Expired verification token for user: {}", user.getEmail());
            throw new IllegalArgumentException("Verification link has expired. Please request a new verification email.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Successfully verified email address for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.info("Resend verification requested for non-existent email: {}", normalizedEmail);
            // Generic response prevents account enumeration
            return;
        }

        User user = userOpt.get();
        if (user.isEmailVerified()) {
            log.info("Resend verification requested for already verified user: {}", normalizedEmail);
            return;
        }

        // Check cooldown to prevent email flooding abuse
        if (user.getEmailVerificationTokenExpiresAt() != null) {
            Instant earliestAllowed = user.getEmailVerificationTokenExpiresAt()
                    .minus(tokenExpirationHours, ChronoUnit.HOURS)
                    .plus(resendCooldownSeconds, ChronoUnit.SECONDS);
            if (Instant.now(clock).isBefore(earliestAllowed)) {
                log.warn("Resend verification requested within cooldown period for: {}", normalizedEmail);
                throw new IllegalStateException("Please wait " + resendCooldownSeconds + " seconds before requesting another verification email.");
            }
        }

        createAndSendVerification(user);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
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
