package com.roomwallah.media;

import com.roomwallah.media.application.service.MediaOrderingServiceImpl;
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

public class MediaOrderingServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMediaRepository propertyMediaRepository;

    @Mock
    private com.roomwallah.user.repository.UserRepository userRepository;

    @InjectMocks
    private MediaOrderingServiceImpl mediaOrderingService;

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
    public void testSuccessfulReorder() {
        UUID mediaId1 = UUID.randomUUID();
        UUID mediaId2 = UUID.randomUUID();

        PropertyMedia media1 = new PropertyMedia();
        media1.setId(mediaId1);
        media1.setPropertyId(propertyId);
        media1.setDisplayOrder(0);

        PropertyMedia media2 = new PropertyMedia();
        media2.setId(mediaId2);
        media2.setPropertyId(propertyId);
        media2.setDisplayOrder(1);

        List<PropertyMedia> mediaList = new ArrayList<>();
        mediaList.add(media1);
        mediaList.add(media2);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId)).thenReturn(mediaList);

        // Reorder list: put mediaId2 first, then mediaId1
        List<UUID> newOrder = List.of(mediaId2, mediaId1);
        mediaOrderingService.reorder(propertyId, ownerId, newOrder);

        assertEquals(1, media1.getDisplayOrder());
        assertEquals(0, media2.getDisplayOrder());
        verify(propertyMediaRepository).saveAll(mediaList);
    }

    @Test
    public void testReorderOwnerMismatch() {
        property.setOwnerId(UUID.randomUUID()); // Mismatched owner
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        assertThrows(IllegalArgumentException.class, () -> 
            mediaOrderingService.reorder(propertyId, ownerId, List.of(UUID.randomUUID()))
        );
    }

    @Test
    public void testReorderForeignMediaItem() {
        UUID mediaId1 = UUID.randomUUID();
        UUID foreignMediaId = UUID.randomUUID();

        PropertyMedia media1 = new PropertyMedia();
        media1.setId(mediaId1);
        media1.setPropertyId(propertyId);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId)).thenReturn(List.of(media1));

        assertThrows(IllegalArgumentException.class, () -> 
            mediaOrderingService.reorder(propertyId, ownerId, List.of(foreignMediaId))
        );
    }
}
