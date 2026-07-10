package com.roomwallah.media;

import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.application.service.CoverImageServiceImpl;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CoverImageServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMediaRepository propertyMediaRepository;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private com.roomwallah.user.repository.UserRepository userRepository;

    @InjectMocks
    private CoverImageServiceImpl coverImageService;

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
    public void testSuccessfulSetCoverImage() {
        UUID mediaId = UUID.randomUUID();
        UUID oldCoverId = UUID.randomUUID();

        PropertyMedia targetMedia = new PropertyMedia();
        targetMedia.setId(mediaId);
        targetMedia.setPropertyId(propertyId);
        targetMedia.setMediaType(MediaType.IMAGE);
        targetMedia.setCover(false);

        PropertyMedia oldCover = new PropertyMedia();
        oldCover.setId(oldCoverId);
        oldCover.setPropertyId(propertyId);
        oldCover.setMediaType(MediaType.IMAGE);
        oldCover.setCover(true);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(targetMedia));
        when(propertyMediaRepository.findByPropertyIdAndIsCoverTrueAndDeletedFalse(propertyId)).thenReturn(List.of(oldCover));
        when(propertyMediaRepository.save(any(PropertyMedia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        coverImageService.setCoverImage(propertyId, ownerId, mediaId);

        assertTrue(targetMedia.isCover());
        assertFalse(oldCover.isCover());
        verify(propertyMediaRepository).save(targetMedia);
        verify(propertyMediaRepository).save(oldCover);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    public void testSetCoverImageNonImage() {
        UUID mediaId = UUID.randomUUID();

        PropertyMedia targetMedia = new PropertyMedia();
        targetMedia.setId(mediaId);
        targetMedia.setPropertyId(propertyId);
        targetMedia.setMediaType(MediaType.VIDEO); // Not an image!

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(targetMedia));

        assertThrows(IllegalArgumentException.class, () -> 
            coverImageService.setCoverImage(propertyId, ownerId, mediaId)
        );
    }

    @Test
    public void testSetCoverImageForeignProperty() {
        UUID mediaId = UUID.randomUUID();

        PropertyMedia targetMedia = new PropertyMedia();
        targetMedia.setId(mediaId);
        targetMedia.setPropertyId(UUID.randomUUID()); // Foreign property
        targetMedia.setMediaType(MediaType.IMAGE);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.findByIdAndDeletedFalse(mediaId)).thenReturn(Optional.of(targetMedia));

        assertThrows(IllegalArgumentException.class, () -> 
            coverImageService.setCoverImage(propertyId, ownerId, mediaId)
        );
    }
}
