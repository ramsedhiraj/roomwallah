package com.roomwallah.user.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.ObjectStoragePort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.event.AvatarUpdatedEvent;
import com.roomwallah.user.presentation.dto.AvatarUploadResponse;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private EventPublisherPort eventPublisher;
    @Mock
    private AuditPort auditPort;

    private AvatarService avatarService;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("john@example.com");
        
        avatarService = new AvatarServiceImpl(userRepository, objectStoragePort, eventPublisher, auditPort);
    }

    @Test
    void uploadAvatar_withValidImage_savesHashedKeyAndReturnsPublicUrl() {
        // Arrange
        byte[] content = "fake-image-bytes".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        String originalFileName = "profile.png";
        String contentType = "image/png";

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectStoragePort.getPublicUrl(anyString())).thenReturn("http://public-url/avatars/new-profile.png");

        // Act
        AvatarUploadResponse response = avatarService.uploadAvatar(testUser, inputStream, originalFileName, contentType);

        // Assert
        assertNotNull(response);
        assertEquals("http://public-url/avatars/new-profile.png", response.getAvatarUrl());
        assertNotNull(response.getAvatarKey());
        assertTrue(response.getAvatarKey().startsWith("avatars/" + userId.toString() + "/avatar_"));
        assertTrue(response.getAvatarKey().endsWith(".png"));

        verify(objectStoragePort).uploadFile(eq(inputStream), eq(response.getAvatarKey()), eq(contentType));
        verify(userRepository).save(testUser);
        verify(eventPublisher).publish(any(AvatarUpdatedEvent.class));
        verify(auditPort).log(eq("USER_AVATAR_UPDATE"), anyString(), anyString(), any());
    }

    @Test
    void uploadAvatar_withUnsupportedContentType_throwsException() {
        // Arrange
        byte[] content = "fake-file-bytes".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        String originalFileName = "doc.pdf";
        String contentType = "application/pdf";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                avatarService.uploadAvatar(testUser, inputStream, originalFileName, contentType)
        );
        verifyNoInteractions(objectStoragePort);
        verifyNoInteractions(userRepository);
    }

    @Test
    void uploadAvatar_whenReplacingExistingAvatar_marksOldKeyForCleanup() {
        // Arrange
        testUser.setAvatarKey("avatars/" + userId.toString() + "/old-avatar.jpg");
        
        byte[] content = "fake-image-bytes".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        String originalFileName = "profile.webp";
        String contentType = "image/webp";

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectStoragePort.getPublicUrl(anyString())).thenReturn("http://public-url/avatars/new-profile.webp");

        // Act
        AvatarUploadResponse response = avatarService.uploadAvatar(testUser, inputStream, originalFileName, contentType);

        // Assert
        assertNotNull(response);
        verify(auditPort).log(eq("AVATAR_CLEANUP_PENDING"), eq(userId.toString()), anyString(), any());
        verify(objectStoragePort, never()).deleteFile(anyString()); // Old avatar key is not deleted immediately
    }
}
