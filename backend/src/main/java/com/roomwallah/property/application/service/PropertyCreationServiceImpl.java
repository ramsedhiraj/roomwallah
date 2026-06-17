package com.roomwallah.property.application.service;

import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyVisibility;
import com.roomwallah.property.domain.event.PropertyCreatedEvent;
import com.roomwallah.property.domain.port.BrokerPolicyPort;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.AreaMeasurement;
import com.roomwallah.property.domain.valueobject.GeoLocation;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyCreationServiceImpl implements PropertyCreationService {

    private final PropertyRepository propertyRepository;
    private final BrokerPolicyPort brokerPolicyPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Property createDraft(User owner, CreatePropertyRequest request) {
        // Enforce active listing count limits
        long activeCount = propertyRepository.countByOwnerIdAndStatusAndDeletedFalse(owner.getId(), PropertyStatus.ACTIVE);
        int maxActive = brokerPolicyPort.getMaxActiveListings(owner);
        if (activeCount >= maxActive) {
            throw new IllegalStateException("Owner has reached the maximum limit of active listings: " + maxActive);
        }

        Property property = new Property();
        property.setOwnerId(owner.getId());
        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setPropertyType(request.getPropertyType());
        property.setListingPurpose(request.getListingPurpose());
        property.setStatus(PropertyStatus.DRAFT);
        
        property.setVisibility(request.getVisibility() != null ? request.getVisibility() : PropertyVisibility.PUBLIC);
        
        property.setPrice(new Money(request.getPrice().getAmount(), request.getPrice().getCurrency()));
        
        if (request.getSecurityDeposit() != null) {
            property.setSecurityDeposit(new Money(request.getSecurityDeposit().getAmount(), request.getSecurityDeposit().getCurrency()));
        }
        if (request.getMaintenanceCharges() != null) {
            property.setMaintenanceCharges(new Money(request.getMaintenanceCharges().getAmount(), request.getMaintenanceCharges().getCurrency()));
        }
        
        property.setNegotiable(request.isNegotiable());
        
        var addrDto = request.getAddress();
        property.setAddress(new Address(
            addrDto.getLine1(),
            addrDto.getLine2(),
            addrDto.getCity(),
            addrDto.getState(),
            addrDto.getCountry(),
            addrDto.getZipCode()
        ));
        
        if (request.getGeoLocation() != null) {
            property.setGeoLocation(new GeoLocation(
                request.getGeoLocation().getLatitude(),
                request.getGeoLocation().getLongitude()
            ));
        }
        
        var areaDto = request.getArea();
        property.setArea(new AreaMeasurement(
            areaDto.getValue(),
            areaDto.getUnit()
        ));
        
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setParkingCount(request.getParkingCount());
        property.setParkingType(request.getParkingType());
        property.setFurnishingStatus(request.getFurnishingStatus());
        
        property.setConstructionYear(request.getConstructionYear());
        property.setFloorNumber(request.getFloorNumber());
        property.setTotalFloors(request.getTotalFloors());
        property.setFacingDirection(request.getFacingDirection());
        property.setPossessionStatus(request.getPossessionStatus());
        property.setPetFriendly(request.isPetFriendly());
        property.setAvailabilityDate(request.getAvailabilityDate());

        property.generateListingRef(addrDto.getCity());

        // Slug generation with uniqueness guarantee
        String baseSlug;
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            baseSlug = request.getSlug().toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        } else {
            baseSlug = generateDefaultSlug(request.getTitle());
        }
        property.setSlug(ensureUniqueSlug(baseSlug));

        // Amenities copy
        if (request.getAmenities() != null) {
            property.getAmenities().addAll(request.getAmenities());
        }

        Property saved = propertyRepository.save(property);

        eventPublisherPort.publish(PropertyCreatedEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now())
            .build());

        return saved;
    }

    private String generateDefaultSlug(String title) {
        if (title == null || title.isBlank()) {
            return "property-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String cleaned = title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.isBlank()) {
            cleaned = "property";
        }
        return cleaned + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String ensureUniqueSlug(String slug) {
        if (!propertyRepository.existsBySlug(slug)) {
            return slug;
        }
        // Slug already taken — append a unique suffix
        String candidate;
        do {
            candidate = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (propertyRepository.existsBySlug(candidate));
        return candidate;
    }
}
