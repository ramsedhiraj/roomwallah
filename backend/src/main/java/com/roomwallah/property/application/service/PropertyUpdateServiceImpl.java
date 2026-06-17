package com.roomwallah.property.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyVisibility;
import com.roomwallah.property.domain.event.PropertyUpdatedEvent;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.AreaMeasurement;
import com.roomwallah.property.domain.valueobject.GeoLocation;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.property.presentation.dto.UpdatePropertyRequest;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyUpdateServiceImpl implements PropertyUpdateService {

    private final PropertyRepository propertyRepository;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Property updateProperty(User owner, UUID propertyId, UpdatePropertyRequest request) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(owner.getId())) {
            throw new IllegalArgumentException("User does not own this property");
        }

        if (property.getStatus() == PropertyStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update an archived property listing");
        }

        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setPropertyType(request.getPropertyType());
        property.setListingPurpose(request.getListingPurpose());
        
        if (request.getVisibility() != null) {
            property.setVisibility(request.getVisibility());
        }
        
        property.setPrice(new Money(request.getPrice().getAmount(), request.getPrice().getCurrency()));
        
        if (request.getSecurityDeposit() != null) {
            property.setSecurityDeposit(new Money(request.getSecurityDeposit().getAmount(), request.getSecurityDeposit().getCurrency()));
        } else {
            property.setSecurityDeposit(null);
        }
        
        if (request.getMaintenanceCharges() != null) {
            property.setMaintenanceCharges(new Money(request.getMaintenanceCharges().getAmount(), request.getMaintenanceCharges().getCurrency()));
        } else {
            property.setMaintenanceCharges(null);
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
        } else {
            property.setGeoLocation(null);
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

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            property.setSlug(request.getSlug().toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-"));
        }

        // Amenities update
        property.getAmenities().clear();
        if (request.getAmenities() != null) {
            property.getAmenities().addAll(request.getAmenities());
        }

        Property saved = propertyRepository.save(property);

        eventPublisherPort.publish(PropertyUpdatedEvent.builder()
            .propertyId(saved.getId())
            .listingRef(saved.getListingRef())
            .ownerId(saved.getOwnerId())
            .updatedAt(Instant.now())
            .build());

        return saved;
    }
}
