package com.roomwallah.recommendation.repository;

import com.roomwallah.recommendation.domain.ListingInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingInteractionRepository extends JpaRepository<ListingInteraction, UUID> {

    @Query("SELECT li.listingId, COUNT(li) FROM ListingInteraction li WHERE li.interactionTime > :since GROUP BY li.listingId ORDER BY COUNT(li) DESC")
    List<Object[]> findTopInteractions(@Param("since") Instant since);
}
