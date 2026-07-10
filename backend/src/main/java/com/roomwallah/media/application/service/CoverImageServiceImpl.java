package com.roomwallah.media.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.event.CoverImageChangedEvent;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CoverImageServiceImpl implements CoverImageService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final EventPublisherPort eventPublisherPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void setCoverImage(UUID propertyId, UUID ownerId, UUID mediaId) {
        // 1. Verify Property ownership
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        checkOwnershipOrAdmin(property, ownerId);

        // 2. Fetch target media
        PropertyMedia media = propertyMediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        if (!media.getPropertyId().equals(propertyId)) {
            throw new IllegalArgumentException("Media item does not belong to this property");
        }

        if (media.getMediaType() != MediaType.IMAGE) {
            throw new IllegalArgumentException("Only images can be marked as cover image");
        }

        // 3. Find previous cover images
        List<PropertyMedia> previousCovers = propertyMediaRepository.findByPropertyIdAndIsCoverTrueAndDeletedFalse(propertyId);
        UUID previousCoverId = previousCovers.isEmpty() ? null : previousCovers.get(0).getId();

        // 4. Unset previous covers
        for (PropertyMedia prev : previousCovers) {
            if (!prev.getId().equals(mediaId)) {
                prev.setCover(false);
                propertyMediaRepository.save(prev);
            }
        }

        // 5. Set target media as cover
        media.setCover(true);
        PropertyMedia savedMedia = propertyMediaRepository.save(media);

        // 6. Publish CoverImageChangedEvent
        eventPublisherPort.publish(CoverImageChangedEvent.builder()
                .propertyId(propertyId)
                .coverMediaId(savedMedia.getId())
                .previousCoverMediaId(previousCoverId)
                .updatedAt(Instant.now())
                .build());
    }

    private void checkOwnershipOrAdmin(Property property, UUID callerId) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + callerId));
        if (!property.getOwnerId().equals(callerId) && caller.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }
    }
}
