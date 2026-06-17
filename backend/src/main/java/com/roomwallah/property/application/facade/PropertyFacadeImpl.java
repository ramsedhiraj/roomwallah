package com.roomwallah.property.application.facade;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.property.application.service.OwnerPropertyService;
import com.roomwallah.property.application.service.PropertyCreationService;
import com.roomwallah.property.application.service.PropertyLifecycleService;
import com.roomwallah.property.application.service.PropertyPublicationService;
import com.roomwallah.property.application.service.PropertyUpdateService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.presentation.dto.AddressDto;
import com.roomwallah.property.presentation.dto.AreaMeasurementDto;
import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.property.presentation.dto.GeoLocationDto;
import com.roomwallah.property.presentation.dto.MoneyDto;
import com.roomwallah.property.presentation.dto.PropertyResponse;
import com.roomwallah.property.presentation.dto.UpdatePropertyRequest;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyFacadeImpl implements PropertyFacade {

    private final CurrentUserProvider currentUserProvider;
    private final PropertyCreationService propertyCreationService;
    private final PropertyUpdateService propertyUpdateService;
    private final PropertyPublicationService propertyPublicationService;
    private final PropertyLifecycleService propertyLifecycleService;
    private final OwnerPropertyService ownerPropertyService;
    private final PropertyRepository propertyRepository;

    @Override
    @Transactional
    public PropertyResponse createDraft(CreatePropertyRequest request) {
        User owner = currentUserProvider.getCurrentUser();
        Property property = propertyCreationService.createDraft(owner, request);
        return mapToResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(UUID propertyId, UpdatePropertyRequest request) {
        User owner = currentUserProvider.getCurrentUser();
        Property property = propertyUpdateService.updateProperty(owner, propertyId, request);
        return mapToResponse(property);
    }

    @Override
    public PropertyResponse getPropertyById(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));
        return mapToResponse(property);
    }

    @Override
    public List<PropertyResponse> getMyProperties() {
        User owner = currentUserProvider.getCurrentUser();
        List<Property> properties = ownerPropertyService.getOwnerProperties(owner);
        return properties.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PropertyResponse submitForVerification(UUID propertyId) {
        User owner = currentUserProvider.getCurrentUser();
        Property property = propertyPublicationService.submitForVerification(owner, propertyId);
        return mapToResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse approveAndPublish(UUID propertyId) {
        Property property = propertyPublicationService.approveAndPublish(propertyId);
        return mapToResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse pauseListing(UUID propertyId) {
        User owner = currentUserProvider.getCurrentUser();
        Property property = propertyLifecycleService.pauseListing(owner, propertyId);
        return mapToResponse(property);
    }

    @Override
    @Transactional
    public PropertyResponse archiveListing(UUID propertyId) {
        User owner = currentUserProvider.getCurrentUser();
        Property property = propertyLifecycleService.archiveListing(owner, propertyId);
        return mapToResponse(property);
    }

    @Override
    @Transactional
    public void deleteProperty(UUID propertyId) {
        User owner = currentUserProvider.getCurrentUser();
        propertyLifecycleService.softDeleteProperty(owner, propertyId);
    }

    private PropertyResponse mapToResponse(Property property) {
        if (property == null) return null;

        MoneyDto priceDto = property.getPrice() != null ?
            new MoneyDto(property.getPrice().getAmount(), property.getPrice().getCurrency()) : null;

        MoneyDto depositDto = property.getSecurityDeposit() != null ?
            new MoneyDto(property.getSecurityDeposit().getAmount(), property.getSecurityDeposit().getCurrency()) : null;

        MoneyDto maintenanceDto = property.getMaintenanceCharges() != null ?
            new MoneyDto(property.getMaintenanceCharges().getAmount(), property.getMaintenanceCharges().getCurrency()) : null;

        AddressDto addressDto = property.getAddress() != null ?
            new AddressDto(
                property.getAddress().getLine1(),
                property.getAddress().getLine2(),
                property.getAddress().getCity(),
                property.getAddress().getState(),
                property.getAddress().getCountry(),
                property.getAddress().getZipCode()
            ) : null;

        GeoLocationDto geoDto = property.getGeoLocation() != null ?
            new GeoLocationDto(property.getGeoLocation().getLatitude(), property.getGeoLocation().getLongitude()) : null;

        AreaMeasurementDto areaDto = property.getArea() != null ?
            new AreaMeasurementDto(property.getArea().getValue(), property.getArea().getUnit()) : null;

        return PropertyResponse.builder()
            .id(property.getId())
            .listingRef(property.getListingRef())
            .ownerId(property.getOwnerId())
            .title(property.getTitle())
            .description(property.getDescription())
            .propertyType(property.getPropertyType())
            .listingPurpose(property.getListingPurpose())
            .status(property.getStatus())
            .visibility(property.getVisibility())
            .price(priceDto)
            .securityDeposit(depositDto)
            .maintenanceCharges(maintenanceDto)
            .negotiable(property.isNegotiable())
            .address(addressDto)
            .geoLocation(geoDto)
            .area(areaDto)
            .bedrooms(property.getBedrooms())
            .bathrooms(property.getBathrooms())
            .parkingCount(property.getParkingCount())
            .parkingType(property.getParkingType())
            .furnishingStatus(property.getFurnishingStatus())
            .constructionYear(property.getConstructionYear())
            .floorNumber(property.getFloorNumber())
            .totalFloors(property.getTotalFloors())
            .facingDirection(property.getFacingDirection())
            .possessionStatus(property.getPossessionStatus())
            .petFriendly(property.isPetFriendly())
            .availabilityDate(property.getAvailabilityDate())
            .publishedAt(property.getPublishedAt())
            .verifiedAt(property.getVerifiedAt())
            .archivedAt(property.getArchivedAt())
            .moderationStatus(property.getModerationStatus())
            .moderationReason(property.getModerationReason())
            .reviewedBy(property.getReviewedBy())
            .reviewedAt(property.getReviewedAt())
            .slug(property.getSlug())
            .amenities(property.getAmenities())
            .createdAt(property.getCreatedAt())
            .updatedAt(property.getUpdatedAt())
            .version(property.getVersion())
            .build();
    }
}
