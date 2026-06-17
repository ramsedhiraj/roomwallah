package com.roomwallah.search.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SearchFilter {
    String city;
    String locality;
    String propertyType;
    String listingPurpose;
    PriceRange priceRange;
    Integer bedrooms;
    Integer bathrooms;
    Boolean petFriendly;
    Boolean ownerVerified;
    GeoRadius geoRadius;
    List<String> amenities;
    String furnishingStatus;
    Integer parkingCount;
    String facingDirection;
    java.time.LocalDate availabilityDate;
    Integer minTrustScore;
    Double bboxNorthEastLat;
    Double bboxNorthEastLon;
    Double bboxSouthWestLat;
    Double bboxSouthWestLon;
}
