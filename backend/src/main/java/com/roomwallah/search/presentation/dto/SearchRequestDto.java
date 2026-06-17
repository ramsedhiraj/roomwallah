package com.roomwallah.search.presentation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SearchRequestDto {
    private String q;
    private String city;
    private String locality;
    private String propertyType;
    private String listingPurpose;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean petFriendly;
    private Boolean ownerVerified;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private String sortBy;
    private String sortDir;
    private String cursor;
    private Integer size;
}
