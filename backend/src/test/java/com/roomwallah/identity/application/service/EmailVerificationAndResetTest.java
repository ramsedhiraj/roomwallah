package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.identity.infrastructure.adapter.PasswordAuthenticationProviderStrategy;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationAndResetTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private Clock clock;
    private Instant fixedNow;

    private EmailVerificationServiceImpl emailVerificationService;
    private PasswordResetServiceImpl passwordResetService;
    private PasswordAuthenticationProviderStrategy authStrategy;

    @BeforeEach
    void setUp() {
        fixedNow = Instant.parse("2026-09-15T10:00:00Z");
        clock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

        emailVerificationService = new EmailVerificationServiceImpl(userRepository, notificationPort, clock);
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(emailVerificationService, "tokenExpirationHours", 24L);
        ReflectionTestUtils.setField(emailVerificationService, "resendCooldownSeconds", 60L);

        passwordResetService = new PasswordResetServiceImpl(userRepository, notificationPort, passwordEncoderPort, clock);
        ReflectionTestUtils.setField(passwordResetService, "resetExpirationMinutes", 15L);

        authStrategy = new PasswordAuthenticationProviderStrategy(userRepository, passwordEncoderPort, clock);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================
    // EMAIL VERIFICATION TESTS
    // ==========================================

    @Test
    @DisplayName("createAndSendVerification: stores SHA-256 hash and sends email")
    void createAndSendVerification_success() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@roomwallah.co.in");
        user.setFullName("Rohan Verma");

        emailVerificationService.createAndSendVerification(user);

        assertNotNull(user.getEmailVerificationTokenHash());
        assertNotNull(user.getEmailVerificationTokenExpiresAt());
        assertEquals(fixedNow.plus(24, ChronoUnit.HOURS), user.getEmailVerificationTokenExpiresAt());

        verify(userRepository).save(user);
        verify(notificationPort).sendEmail(eq("test@roomwallah.co.in"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("verifyEmailToken: marks user as verified and clears token")
    void verifyEmailToken_success() {
        String rawToken = "my_secure_verification_token";
        String tokenHash = sha256(rawToken);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@roomwallah.co.in");
        user.setEmailVerified(false);
        user.setEmailVerificationTokenHash(tokenHash);
        user.setEmailVerificationTokenExpiresAt(fixedNow.plus(10, ChronoUnit.HOURS));

        when(userRepository.findByEmailVerificationTokenHash(tokenHash)).thenReturn(Optional.of(user));

        emailVerificationService.verifyEmailToken(rawToken);

        assertTrue(user.isEmailVerified());
        assertEquals(fixedNow, user.getEmailVerifiedAt());
        assertNull(user.getEmailVerificationTokenHash());
        assertNull(user.getEmailVerificationTokenExpiresAt());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("verifyEmailToken: rejects invalid or reused token")
    void verifyEmailToken_invalidToken() {
        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                emailVerificationService.verifyEmailToken("unknown_token"));

        assertTrue(ex.getMessage().contains("invalid or has already been used"));
    }

    @Test
    @DisplayName("verifyEmailToken: rejects expired token")
    void verifyEmailToken_expiredToken() {
        String rawToken = "expired_token";
        String tokenHash = sha256(rawToken);

        User user = new User();
        user.setEmail("test@roomwallah.co.in");
        user.setEmailVerificationTokenHash(tokenHash);
        user.setEmailVerificationTokenExpiresAt(fixedNow.minus(1, ChronoUnit.HOURS)); // Expired

        when(userRepository.findByEmailVerificationTokenHash(tokenHash)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                emailVerificationService.verifyEmailToken(rawToken));

        assertTrue(ex.getMessage().contains("expired"));
        assertFalse(user.isEmailVerified());
    }

    @Test
    @DisplayName("resendVerificationEmail: enforces 60-second cooldown")
    void resendVerificationEmail_cooldownActive() {
        User user = new User();
        user.setEmail("test@roomwallah.co.in");
        user.setEmailVerified(false);
        // Token was generated 10 seconds ago (expires in 24h minus 10s)
        user.setEmailVerificationTokenExpiresAt(fixedNow.plus(24, ChronoUnit.HOURS).minus(10, ChronoUnit.SECONDS));

        when(userRepository.findByEmail("test@roomwallah.co.in")).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emailVerificationService.resendVerificationEmail("test@roomwallah.co.in"));

        assertTrue(ex.getMessage().contains("Please wait"));
        verify(notificationPort, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("resendVerificationEmail: non-existent email returns quietly")
    void resendVerificationEmail_nonExistentUser_returnsQuietly() {
        when(userRepository.findByEmail("ghost@roomwallah.co.in")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> emailVerificationService.resendVerificationEmail("ghost@roomwallah.co.in"));
        verify(notificationPort, never()).sendEmail(any(), any(), any(), any());
    }

    // ==========================================
    // PASSWORD RESET TESTS
    // ==========================================

    @Test
    @DisplayName("forgotPassword: generates code and sends email for existing user")
    void forgotPassword_success() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@roomwallah.co.in");
        user.setFullName("Priya Sharma");
        user.setDeleted(false);

        when(userRepository.findByEmail("user@roomwallah.co.in")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword("user@roomwallah.co.in");

        assertNotNull(user.getPasswordResetTokenHash());
        assertNotNull(user.getPasswordResetTokenExpiresAt());
        assertEquals(fixedNow.plus(15, ChronoUnit.MINUTES), user.getPasswordResetTokenExpiresAt());

        verify(userRepository).save(user);
        verify(notificationPort).sendEmail(eq("user@roomwallah.co.in"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("forgotPassword: non-existent email returns quietly without error")
    void forgotPassword_nonExistentEmail_returnsQuietly() {
        when(userRepository.findByEmail("unknown@roomwallah.co.in")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.forgotPassword("unknown@roomwallah.co.in"));
        verify(notificationPort, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("resetPassword: valid code updates password and invalidates token")
    void resetPassword_validCode_success() {
        String rawCode = "123456";
        String codeHash = sha256(rawCode);

        User user = new User();
        user.setEmail("user@roomwallah.co.in");
        user.setPasswordHash("old_hash");
        user.setPasswordResetTokenHash(codeHash);
        user.setPasswordResetTokenExpiresAt(fixedNow.plus(10, ChronoUnit.MINUTES));
        user.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("user@roomwallah.co.in")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.encode("NewSecret123!")).thenReturn("new_hash");

        passwordResetService.resetPassword("user@roomwallah.co.in", rawCode, "NewSecret123!", "NewSecret123!");

        assertEquals("new_hash", user.getPasswordHash());
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetTokenExpiresAt());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword: wrong code throws exception and does not reset password")
    void resetPassword_invalidCode_throwsException() {
        String rawCode = "123456";
        String codeHash = sha256(rawCode);

        User user = new User();
        user.setEmail("user@roomwallah.co.in");
        user.setPasswordHash("old_hash");
        user.setPasswordResetTokenHash(codeHash);
        user.setPasswordResetTokenExpiresAt(fixedNow.plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("user@roomwallah.co.in")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                passwordResetService.resetPassword("user@roomwallah.co.in", "999999", "NewSecret123!", "NewSecret123!"));

        assertTrue(ex.getMessage().contains("Invalid or expired"));
        assertEquals("old_hash", user.getPasswordHash());
    }

    @Test
    @DisplayName("resetPassword: expired code throws exception")
    void resetPassword_expiredCode_throwsException() {
        String rawCode = "123456";
        String codeHash = sha256(rawCode);

        User user = new User();
        user.setEmail("user@roomwallah.co.in");
        user.setPasswordHash("old_hash");
        user.setPasswordResetTokenHash(codeHash);
        user.setPasswordResetTokenExpiresAt(fixedNow.minus(2, ChronoUnit.MINUTES)); // Expired

        when(userRepository.findByEmail("user@roomwallah.co.in")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                passwordResetService.resetPassword("user@roomwallah.co.in", rawCode, "NewSecret123!", "NewSecret123!"));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals("old_hash", user.getPasswordHash());
    }

    // ==========================================
    // AUTHENTICATION GUARDRAILS TESTS
    // ==========================================

    @Test
    @DisplayName("authenticate: rejects unverified user with clear error when password matches")
    void authenticate_unverifiedUser_throwsClearException() {
        User user = new User();
        user.setEmail("unverified@roomwallah.co.in");
        user.setPasswordHash("hashed_pwd");
        user.setEmailVerified(false);
        user.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("unverified@roomwallah.co.in")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("ValidPwd123!", "hashed_pwd")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authStrategy.authenticate("unverified@roomwallah.co.in", "ValidPwd123!"));

        assertTrue(ex.getMessage().contains("Please verify your email address before logging in"));
    }

    @Test
    @DisplayName("authenticate: verified user with valid password succeeds")
    void authenticate_verifiedUser_succeeds() {
        User user = new User();
        user.setEmail("verified@roomwallah.co.in");
        user.setPasswordHash("hashed_pwd");
        user.setEmailVerified(true);
        user.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("verified@roomwallah.co.in")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("ValidPwd123!", "hashed_pwd")).thenReturn(true);

        User result = authStrategy.authenticate("verified@roomwallah.co.in", "ValidPwd123!");

        assertNotNull(result);
        assertEquals("verified@roomwallah.co.in", result.getEmail());
    }

    @Test
    @DisplayName("authenticate: invalid password throws invalid credentials error without revealing verification status")
    void authenticate_invalidPassword_throwsGenericError() {
        User user = new User();
        user.setEmail("user@roomwallah.co.in");
        user.setPasswordHash("hashed_pwd");
        user.setEmailVerified(false);
        user.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("user@roomwallah.co.in")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("WrongPassword!", "hashed_pwd")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authStrategy.authenticate("user@roomwallah.co.in", "WrongPassword!"));

        assertEquals("Invalid email/phone or password", ex.getMessage());
    }
}
