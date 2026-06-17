package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(userRegistrationService, passwordEncoderPort);
    }

    @Test
    void register_withValidParameters_encodesAndDelegatesSave() {
        // Arrange
        String fullName = "Alice Smith";
        String email = "alice@example.com";
        String phone = "+1234567890";
        String password = "Password123!"; // Valid: 8+ chars, upper, lower, digit, special
        UserRole role = UserRole.TENANT;

        User mockUser = new User();
        mockUser.setFullName(fullName);
        mockUser.setEmail(email);
        mockUser.setPhone(phone);
        mockUser.setPasswordHash("encoded_password_hash");
        mockUser.setRole(role);

        when(passwordEncoderPort.encode(password)).thenReturn("encoded_password_hash");
        when(userRegistrationService.registerUser(eq(fullName), eq(email), eq(phone), eq("encoded_password_hash"), eq(role)))
                .thenReturn(mockUser);

        // Act
        User result = registrationService.register(fullName, email, phone, password, role);

        // Assert
        assertNotNull(result);
        assertEquals(fullName, result.getFullName());
        assertEquals(email, result.getEmail());
        assertEquals("encoded_password_hash", result.getPasswordHash());
        verify(passwordEncoderPort).encode(password);
        verify(userRegistrationService).registerUser(fullName, email, phone, "encoded_password_hash", role);
    }

    @Test
    void register_withInvalidPasswordPolicy_throwsException() {
        // Arrange
        String fullName = "Alice Smith";
        String email = "alice@example.com";
        String phone = "+1234567890";
        String password = "weak"; // Fails policy
        UserRole role = UserRole.TENANT;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                registrationService.register(fullName, email, phone, password, role)
        );

        assertNotNull(exception.getMessage());
        verifyNoInteractions(passwordEncoderPort);
        verifyNoInteractions(userRegistrationService);
    }
}
