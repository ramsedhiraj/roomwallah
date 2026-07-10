package com.roomwallah.property.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.AreaMeasurement;
import com.roomwallah.property.domain.valueobject.GeoLocation;
import com.roomwallah.property.domain.valueobject.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
public class Property extends BaseEntity {

    @Column(name = "listing_ref", nullable = false, unique = true, length = 100)
    private String listingRef;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 50)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_purpose", nullable = false, length = 50)
    private ListingPurpose listingPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PropertyStatus status = PropertyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 50)
    private PropertyVisibility visibility = PropertyVisibility.PUBLIC;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "price_currency", nullable = false, length = 10))
    })
    private Money price;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "security_deposit_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "security_deposit_currency", length = 10))
    })
    private Money securityDeposit;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "maintenance_charges_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "maintenance_charges_currency", length = 10))
    })
    private Money maintenanceCharges;

    @Column(name = "negotiable", nullable = false)
    private boolean negotiable = false;

    @Embedded
    private Address address;

    @Embedded
    private GeoLocation geoLocation;

    @Embedded
    private AreaMeasurement area;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "parking_count", nullable = false)
    private int parkingCount = 0;

    @Column(name = "parking_type", length = 50)
    private String parkingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing_status", length = 50)
    private FurnishingStatus furnishingStatus;

    // Enriched Optional Metadata
    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "facing_direction", length = 50)
    private String facingDirection;

    @Column(name = "possession_status", length = 50)
    private String possessionStatus;

    @Column(name = "pet_friendly", nullable = false)
    private boolean petFriendly = false;

    @Column(name = "availability_date")
    private LocalDate availabilityDate;

    // Lifecycle Timestamps
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    // Moderation Metadata
    @Column(name = "moderation_status", length = 50)
    private String moderationStatus;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // SEO Friendly Slug
    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    // Extensible list of amenities (Lift, Gym, Balcony, etc.)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_amenities", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "amenity")
    private Set<String> amenities = new HashSet<>();

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    public void transitionTo(PropertyStatus newStatus) {
        if (this.status == null) {
            this.status = PropertyStatus.DRAFT;
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public void generateListingRef(String city) {
        String cleanCity = city != null ? city.replaceAll("[^a-zA-Z]", "").toUpperCase() : "IND";
        if (cleanCity.isBlank()) {
            cleanCity = "IND";
        }
        int year = java.time.LocalDate.now().getYear();
        long random = (long) (Math.random() * 900000L) + 100000L; // 6 digit random number
        this.listingRef = "RW-" + cleanCity + "-" + year + "-" + random;
    }
}
