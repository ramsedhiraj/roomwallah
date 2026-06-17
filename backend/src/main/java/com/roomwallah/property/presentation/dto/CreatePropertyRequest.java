package com.roomwallah.property.presentation.dto;

import com.roomwallah.property.domain.entity.FurnishingStatus;
import com.roomwallah.property.domain.entity.ListingPurpose;
import com.roomwallah.property.domain.entity.PropertyType;
import com.roomwallah.property.domain.entity.PropertyVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePropertyRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Listing purpose is required")
    private ListingPurpose listingPurpose;

    private PropertyVisibility visibility;

    @NotNull(message = "Price is required")
    @Valid
    private MoneyDto price;

    @Valid
    private MoneyDto securityDeposit;

    @Valid
    private MoneyDto maintenanceCharges;

    private boolean negotiable;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    @Valid
    private GeoLocationDto geoLocation;

    @NotNull(message = "Area measurement is required")
    @Valid
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

    // SEO Friendly Slug
    private String slug;

    // Extensible amenities list
    private Set<String> amenities = new HashSet<>();
}
