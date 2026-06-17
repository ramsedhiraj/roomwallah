package com.roomwallah.recommendation.repository;

import com.roomwallah.recommendation.domain.ListingVector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ListingVectorRepository extends JpaRepository<ListingVector, UUID> {
    Optional<ListingVector> findByListingId(UUID listingId);
}
