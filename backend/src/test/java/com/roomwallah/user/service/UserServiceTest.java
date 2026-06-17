package com.roomwallah.user.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.ObjectStoragePort;
import com.roomwallah.identity.domain.port.PasswordEncoderPort;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserPreferences;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.event.AccountDeactivatedEvent;
import com.roomwallah.user.event.PasswordChangedEvent;
import com.roomwallah.user.event.UserProfileUpdatedEvent;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;
import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventPublisherPort eventPublisher;
    @Mock
    private AuditPort auditPort;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private UserProfileService userProfileService;
    private PasswordManagementService passwordManagementService;
    private AccountLifecycleService accountLifecycleService;
    private OwnerProfileService ownerProfileService;

    private User testUser;
    private UserPreferences testPrefs;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        testUser = new User();
        testUser.setId(userId);
        testUser.setFullName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPhone("+123456789");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole(UserRole.TENANT);
        testUser.setStatus(AccountStatus.ACTIVE);
        
        testPrefs = new UserPreferences();
        testPrefs.setUser(testUser);
        testPrefs.setDarkModePreferred(false);
        testPrefs.setEmailNotificationsEnabled(true);
        testUser.setPreferences(testPrefs);

        userProfileService = new UserProfileServiceImpl(userRepository, eventPublisher, auditPort, objectStoragePort);
        passwordManagementService = new PasswordManagementServiceImpl(userRepository, passwordEncoderPort, eventPublisher, auditPort);
        accountLifecycleService = new AccountLifecycleServiceImpl(userRepository, eventPublisher, auditPort);
        ownerProfileService = new OwnerProfileServiceImpl(userRepository, objectStoragePort);
    }

    @Test
    void getProfile_returnsCorrectResponse() {
        // Arrange
        testUser.setAvatarKey("avatars/123/avatar.png");
        when(objectStoragePort.getPublicUrl("avatars/123/avatar.png")).thenReturn("http://public-url/avatar.png");

        // Act
        UserProfileResponse response = userProfileService.getProfile(testUser);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("John Doe", response.getFullName());
        assertEquals("http://public-url/avatar.png", response.getAvatarUrl());
        assertTrue(response.isEmailNotificationsEnabled());
    }

    @Test
    void updateProfile_updatesPersonalAndPreferencesFields() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .bio("My bio")
                .darkModePreferred(true)
                .emailNotificationsEnabled(false)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserProfileResponse response = userProfileService.updateProfile(testUser, request);

        // Assert
        assertNotNull(response);
        assertEquals("John Updated", testUser.getFullName());
        assertEquals("My bio", testUser.getBio());
        assertTrue(testPrefs.isDarkModePreferred());
        assertFalse(testPrefs.isEmailNotificationsEnabled());
        
        verify(userRepository).save(testUser);
        verify(eventPublisher).publish(any(UserProfileUpdatedEvent.class));
        verify(auditPort).log(eq("USER_PROFILE_UPDATE"), anyString(), anyString(), any());
    }

    @Test
    void changePassword_withValidCurrentAndNewPassword_savesAndPublishesEvent() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        when(passwordEncoderPort.matches("OldPassword123!", "hashed_password")).thenReturn(true);
        when(passwordEncoderPort.encode("NewPassword123!")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        passwordManagementService.changePassword(testUser, request);

        // Assert
        assertEquals("new_hashed_password", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
        verify(eventPublisher).publish(any(PasswordChangedEvent.class));
        verify(auditPort).log(eq("USER_PASSWORD_CHANGE"), anyString(), anyString(), any());
    }

    @Test
    void changePassword_withIncorrectCurrentPassword_throwsException() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        when(passwordEncoderPort.matches("WrongPassword123!", "hashed_password")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                passwordManagementService.changePassword(testUser, request)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_withComplexityFailure_throwsException() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("weak")
                .confirmPassword("weak")
                .build();

        when(passwordEncoderPort.matches("OldPassword123!", "hashed_password")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                passwordManagementService.changePassword(testUser, request)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateAccount_softDeletesUserAndDisablesStatus() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        accountLifecycleService.deactivateAccount(testUser);

        // Assert
        assertTrue(testUser.isDeleted());
        assertEquals(AccountStatus.DISABLED, testUser.getStatus());
        assertNotNull(testUser.getDeletedAt());
        verify(userRepository).save(testUser);
        verify(eventPublisher).publish(any(AccountDeactivatedEvent.class));
        verify(auditPort).log(eq("USER_ACCOUNT_DEACTIVATION"), anyString(), anyString(), any());
    }

    @Test
    void getPublicOwnerProfile_returnsPublicInformationOnly() {
        // Arrange
        testUser.setRole(UserRole.OWNER);
        testUser.setAvatarKey("avatars/456/owner.png");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(objectStoragePort.getPublicUrl("avatars/456/owner.png")).thenReturn("http://public-url/owner.png");

        // Act
        PublicOwnerProfileResponse response = ownerProfileService.getPublicOwnerProfile(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("John Doe", response.getDisplayName());
        assertEquals("http://public-url/owner.png", response.getAvatarUrl());
        assertEquals(85, response.getTrustScore());
    }

    @Test
    void getPublicOwnerProfile_forNonOwner_throwsException() {
        // Arrange
        testUser.setRole(UserRole.TENANT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                ownerProfileService.getPublicOwnerProfile(userId)
        );
    }
}
