package com.roomwallah.media;

import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.application.service.MediaDeletionServiceImpl;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MediaDeletionServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMediaRepository propertyMediaRepository;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private com.roomwallah.user.repository.UserRepository userRepository;

    @InjectMocks
    private MediaDeletionServiceImpl mediaDeletionService;

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
    }

    @Test
    public void testSuccessfulSoftDelete() {
        UUID mediaId = UUID.randomUUID();
        PropertyMedia media = new PropertyMedia();
        media.setId(mediaId);
        media.setPropertyId(propertyId);
        media.setObjectKey("properties/123/image.png");
        media.setDeleted(false);

        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.save(any(PropertyMedia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mediaDeletionService.delete(mediaId, ownerId);

        assertTrue(media.isDeleted());
        assertNotNull(media.getDeletedAt());
        verify(propertyMediaRepository).save(media);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    public void testDeleteOwnerMismatch() {
        UUID mediaId = UUID.randomUUID();
        PropertyMedia media = new PropertyMedia();
        media.setId(mediaId);
        media.setPropertyId(propertyId);

        property.setOwnerId(UUID.randomUUID()); // Mismatched owner

        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        assertThrows(IllegalArgumentException.class, () -> 
            mediaDeletionService.delete(mediaId, ownerId)
        );
    }
}
