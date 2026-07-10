package com.roomwallah.property.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.event.PropertyPublishedEvent;
import com.roomwallah.property.domain.event.PropertySubmittedForVerificationEvent;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyPublicationServiceImpl implements PropertyPublicationService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Property submitForVerification(User owner, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(owner.getId()) && owner.getRole() != com.roomwallah.user.entity.UserRole.ADMIN) {
            throw new IllegalArgumentException("User does not own this property");
        }

        property.transitionTo(PropertyStatus.PENDING_VERIFICATION);
        Property saved = propertyRepository.saveAndFlush(property);

        eventPublisherPort.publish(PropertySubmittedForVerificationEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .submittedAt(Instant.now())
            .build());

        return saved;
    }

    @Override
    @Transactional
    public Property approveAndPublish(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        User owner = userRepository.findById(property.getOwnerId())
            .orElseThrow(() -> new IllegalStateException("Owner not found for property"));

        // Verification Gates Check
        if (!owner.isEmailVerified()) {
            throw new IllegalStateException("Owner email must be verified before listing publication");
        }
        if (!owner.isPhoneVerified()) {
            throw new IllegalStateException("Owner phone number must be verified before listing publication");
        }
        if (!owner.isIdentityVerified()) {
            throw new IllegalStateException("Owner identity must be verified before listing publication");
        }

        property.transitionTo(PropertyStatus.ACTIVE);
        
        Instant now = Instant.now();
        property.setVerifiedAt(now);
        property.setPublishedAt(now);
        
        Property saved = propertyRepository.saveAndFlush(property);

        eventPublisherPort.publish(PropertyPublishedEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .publishedAt(now)
            .build());

        return saved;
    }
}
