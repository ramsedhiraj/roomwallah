package com.roomwallah.property.domain.repository;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    
    @Override
    @EntityGraph(attributePaths = {"amenities"})
    Optional<Property> findById(UUID id);

    @EntityGraph(attributePaths = {"amenities"})
    List<Property> findByOwnerIdAndDeletedFalse(UUID ownerId);

    @EntityGraph(attributePaths = {"amenities"})
    Optional<Property> findByListingRefAndDeletedFalse(String listingRef);

    long countByOwnerIdAndStatusAndDeletedFalse(UUID ownerId, PropertyStatus status);

    boolean existsBySlug(String slug);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Property p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}
