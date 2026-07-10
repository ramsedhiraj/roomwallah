package com.roomwallah.media.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.event.MediaDeletedEvent;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class MediaDeletionServiceImpl implements MediaDeletionService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final EventPublisherPort eventPublisherPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void delete(UUID mediaId, UUID ownerId) {
        // 1. Fetch media
        PropertyMedia media = propertyMediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        // 2. Fetch Property and verify ownership
        Property property = propertyRepository.findById(media.getPropertyId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + media.getPropertyId()));

        User caller = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + ownerId));
        if (!property.getOwnerId().equals(ownerId) && caller.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // 3. Perform soft delete
        media.softDelete();
        PropertyMedia saved = propertyMediaRepository.save(media);

        // 4. Publish MediaDeletedEvent
        eventPublisherPort.publish(MediaDeletedEvent.builder()
                .mediaId(saved.getId())
                .propertyId(saved.getPropertyId())
                .objectKey(saved.getObjectKey())
                .deletedAt(saved.getDeletedAt() != null ? saved.getDeletedAt() : Instant.now())
                .build());
    }
}
