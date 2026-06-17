package com.roomwallah.property.presentation.dto;

import com.roomwallah.property.domain.entity.FurnishingStatus;
import com.roomwallah.property.domain.entity.ListingPurpose;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyType;
import com.roomwallah.property.domain.entity.PropertyVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {
    private UUID id;
    private String listingRef;
    private UUID ownerId;
    private String title;
    private String description;
    private PropertyType propertyType;
    private ListingPurpose listingPurpose;
    private PropertyStatus status;
    private PropertyVisibility visibility;
    private MoneyDto price;
    private MoneyDto securityDeposit;
    private MoneyDto maintenanceCharges;
    private boolean negotiable;
    private AddressDto address;
    private GeoLocationDto geoLocation;
    private AreaMeasurementDto area;
    private Integer bedrooms;
    private Integer bathrooms;
    private int parkingCount;
    private String parkingType;
    private FurnishingStatus furnishingStatus;
    private Integer constructionYear;
    private Integer floorNumber;
    private Integer totalFloors;
    private String facingDirection;
    private String possessionStatus;
    private boolean petFriendly;
    private LocalDate availabilityDate;
    
    // Lifecycle Timestamps
    private Instant publishedAt;
    private Instant verifiedAt;
    private Instant archivedAt;

    // Moderation Metadata
    private String moderationStatus;
    private String moderationReason;
    private UUID reviewedBy;
    private Instant reviewedAt;

    // SEO Friendly Slug
    private String slug;

    // Extensible amenities list
    private Set<String> amenities;

    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
