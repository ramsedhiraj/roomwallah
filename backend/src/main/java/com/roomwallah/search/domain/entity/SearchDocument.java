package com.roomwallah.search.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "search_documents")
@Getter
@Setter
public class SearchDocument implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "property_id")
    private UUID propertyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "listing_ref", nullable = false, length = 100)
    private String listingRef;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "locality", nullable = false, length = 100)
    private String locality;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;

    @Column(name = "listing_purpose", nullable = false, length = 50)
    private String listingPurpose;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "parking_count", nullable = false)
    private int parkingCount = 0;

    @Column(name = "furnishing_status", length = 50)
    private String furnishingStatus;

    @Column(name = "pet_friendly", nullable = false)
    private boolean petFriendly = false;

    @Column(name = "trust_score", nullable = false)
    private int trustScore = 0;

    @Column(name = "owner_verified", nullable = false)
    private boolean ownerVerified = false;

    @Column(name = "owner_badge", length = 50)
    private String ownerBadge;

    @Column(name = "property_status", nullable = false, length = 50)
    private String propertyStatus;

    @Column(name = "media_count", nullable = false)
    private int mediaCount = 0;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "event_version", nullable = false)
    private long eventVersion = 1;

    @Column(name = "last_event_timestamp", nullable = false)
    private Instant lastEventTimestamp;

    @Column(name = "facing_direction", length = 50)
    private String facingDirection;

    @Column(name = "availability_date")
    private java.time.LocalDate availabilityDate;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Transient
    private Map<String, Object> rankingExplanation;
}
