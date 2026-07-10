package com.roomwallah.property.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.event.PropertyArchivedEvent;
import com.roomwallah.property.domain.event.PropertyPausedEvent;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyLifecycleServiceImpl implements PropertyLifecycleService {

    private final PropertyRepository propertyRepository;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Property pauseListing(User owner, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(owner.getId()) && owner.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }

        property.transitionTo(PropertyStatus.PAUSED);
        Property saved = propertyRepository.saveAndFlush(property);

        eventPublisherPort.publish(PropertyPausedEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .pausedAt(Instant.now())
            .build());

        return saved;
    }

    @Override
    @Transactional
    public Property archiveListing(User owner, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(owner.getId()) && owner.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }

        property.transitionTo(PropertyStatus.ARCHIVED);
        
        Instant now = Instant.now();
        property.setArchivedAt(now);
        
        Property saved = propertyRepository.saveAndFlush(property);

        eventPublisherPort.publish(PropertyArchivedEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .archivedAt(now)
            .build());

        return saved;
    }

    @Override
    @Transactional
    public void softDeleteProperty(User owner, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(owner.getId()) && owner.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }

        property.setDeleted(true);
        property.setDeletedAt(Instant.now());
        propertyRepository.saveAndFlush(property);
    }
}
