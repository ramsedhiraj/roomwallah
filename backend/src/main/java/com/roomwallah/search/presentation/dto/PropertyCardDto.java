package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class PropertyCardDto {
    UUID propertyId;
    String listingRef;
    String slug;
    String title;
    String city;
    String locality;
    BigDecimal price;
    String propertyType;
    String listingPurpose;
    Integer bedrooms;
    Integer bathrooms;
    int parkingCount;
    String furnishingStatus;
    boolean petFriendly;
    int trustScore;
    boolean ownerVerified;
    String ownerBadge;
    int mediaCount;
    Instant publishedAt;
    String thumbnailUrl;
    Double latitude;
    Double longitude;
    Map<String, Object> rankingExplanation;
}
