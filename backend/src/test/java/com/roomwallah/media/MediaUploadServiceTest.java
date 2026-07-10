package com.roomwallah.media;

import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.application.service.MediaUploadServiceImpl;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.port.MediaPolicyPort;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.repository.UploadIdempotencyRepository;
import com.roomwallah.media.domain.port.UploadSessionPort;
import com.roomwallah.media.domain.valueobject.FileChecksum;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MediaUploadServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMediaRepository propertyMediaRepository;

    @Mock
    private MediaStoragePort mediaStoragePort;

    @Mock
    private MediaPolicyPort mediaPolicyPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private UploadIdempotencyRepository uploadIdempotencyRepository;

    @Mock
    private UploadSessionPort uploadSessionPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MediaUploadServiceImpl mediaUploadService;

    private UUID propertyId;
    private UUID ownerId;
    private Property property;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        propertyId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        
        property = new Property();
        property.setId(propertyId);
        property.setOwnerId(ownerId);
        property.setDeleted(false);

        // Mock default behavior for userRepository
        com.roomwallah.user.entity.User caller = new com.roomwallah.user.entity.User();
        caller.setId(ownerId);
        caller.setRole(com.roomwallah.user.entity.UserRole.OWNER);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(caller));

        // Default mock behaviors for mediaPolicyPort limits
        when(mediaPolicyPort.getMaxImagesPerProperty()).thenReturn(20);
        when(mediaPolicyPort.getMaxVideosPerProperty()).thenReturn(5);
        when(mediaPolicyPort.getMaxFloorPlansPerProperty()).thenReturn(5);
        when(mediaPolicyPort.getMaxVirtualToursPerProperty()).thenReturn(1);
    }

    @Test
    public void testSuccessfulUpload() {
        byte[] content = "fake image content".getBytes();
        
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(mediaPolicyPort.isSupportedMimeType(MediaType.IMAGE, "image/jpeg")).thenReturn(true);
        when(mediaPolicyPort.isSupportedExtension(MediaType.IMAGE, "jpg")).thenReturn(true);
        when(mediaPolicyPort.getMaxFileSize(MediaType.IMAGE)).thenReturn(1000L);
        when(propertyMediaRepository.existsByPropertyIdAndChecksumChecksumSha256AndDeletedFalse(eq(propertyId), anyString())).thenReturn(false);
        when(propertyMediaRepository.countByPropertyIdAndMediaTypeAndDeletedFalse(propertyId, MediaType.IMAGE)).thenReturn(0L);
        when(propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId)).thenReturn(new ArrayList<>());
        
        PropertyMedia mockSaved = new PropertyMedia();
        mockSaved.setId(UUID.randomUUID());
        mockSaved.setPropertyId(propertyId);
        mockSaved.setObjectKey("fake-key");
        mockSaved.setMediaType(MediaType.IMAGE);
        mockSaved.setChecksum(new FileChecksum("fake-checksum"));
        
        when(propertyMediaRepository.save(any(PropertyMedia.class))).thenReturn(mockSaved);

        PropertyMedia result = mediaUploadService.upload(propertyId, ownerId, content, "test.jpg", "image/jpeg", MediaType.IMAGE, "test-idempotency-key");
        
        assertNotNull(result);
        verify(mediaStoragePort).store(anyString(), any(InputStream.class), eq("image/jpeg"), eq((long) content.length));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    public void testUploadOwnerMismatch() {
        byte[] content = "content".getBytes();
        property.setOwnerId(UUID.randomUUID()); // Mismatched owner

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        assertThrows(IllegalArgumentException.class, () -> 
            mediaUploadService.upload(propertyId, ownerId, content, "test.jpg", "image/jpeg", MediaType.IMAGE, "test-idempotency-key")
        );
    }

    @Test
    public void testUploadMimeTypeNotSupported() {
        byte[] content = "content".getBytes();

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(mediaPolicyPort.isSupportedMimeType(MediaType.IMAGE, "application/pdf")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            mediaUploadService.upload(propertyId, ownerId, content, "test.pdf", "application/pdf", MediaType.IMAGE, "test-idempotency-key")
        );
    }

    @Test
    public void testUploadDuplicateChecksum() {
        byte[] content = "content".getBytes();

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(mediaPolicyPort.isSupportedMimeType(MediaType.IMAGE, "image/jpeg")).thenReturn(true);
        when(mediaPolicyPort.isSupportedExtension(MediaType.IMAGE, "jpg")).thenReturn(true);
        when(mediaPolicyPort.getMaxFileSize(MediaType.IMAGE)).thenReturn(1000L);
        when(propertyMediaRepository.existsByPropertyIdAndChecksumChecksumSha256AndDeletedFalse(eq(propertyId), anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            mediaUploadService.upload(propertyId, ownerId, content, "test.jpg", "image/jpeg", MediaType.IMAGE, "test-idempotency-key")
        );
    }

    @Test
    public void testUploadLimitExceeded() {
        byte[] content = "content".getBytes();

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(mediaPolicyPort.isSupportedMimeType(MediaType.IMAGE, "image/jpeg")).thenReturn(true);
        when(mediaPolicyPort.isSupportedExtension(MediaType.IMAGE, "jpg")).thenReturn(true);
        when(mediaPolicyPort.getMaxFileSize(MediaType.IMAGE)).thenReturn(1000L);
        when(propertyMediaRepository.existsByPropertyIdAndChecksumChecksumSha256AndDeletedFalse(eq(propertyId), anyString())).thenReturn(false);
        // Say limit is 20, and count is 20
        when(propertyMediaRepository.countByPropertyIdAndMediaTypeAndDeletedFalse(propertyId, MediaType.IMAGE)).thenReturn(20L);
        when(mediaPolicyPort.getMaxImagesPerProperty()).thenReturn(20);

        assertThrows(IllegalArgumentException.class, () -> 
            mediaUploadService.upload(propertyId, ownerId, content, "test.jpg", "image/jpeg", MediaType.IMAGE, "test-idempotency-key")
        );
    }
}
