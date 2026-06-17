package com.roomwallah.media.domain.repository;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.entity.MediaType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, UUID> {
    
    @EntityGraph(attributePaths = {"derivatives"})
    List<PropertyMedia> findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(UUID propertyId);

    @Override
    @EntityGraph(attributePaths = {"derivatives"})
    Optional<PropertyMedia> findById(UUID id);

    @EntityGraph(attributePaths = {"derivatives"})
    Optional<PropertyMedia> findByIdAndDeletedFalse(UUID id);

    boolean existsByPropertyIdAndChecksumChecksumSha256AndDeletedFalse(UUID propertyId, String checksumSha256);
    List<PropertyMedia> findByPropertyIdAndIsCoverTrueAndDeletedFalse(UUID propertyId);
    long countByPropertyIdAndMediaTypeAndDeletedFalse(UUID propertyId, MediaType mediaType);
    List<PropertyMedia> findByDeletedTrueAndDeletedAtBefore(Instant cutoff);
}
