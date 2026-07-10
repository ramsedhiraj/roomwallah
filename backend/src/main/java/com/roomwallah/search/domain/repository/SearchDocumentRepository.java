package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.SearchDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SearchDocumentRepository extends JpaRepository<SearchDocument, UUID> {

    Optional<SearchDocument> findByPropertyId(UUID propertyId);

    List<SearchDocument> findByPropertyStatus(String status);

    List<SearchDocument> findByCity(String city);

    long countByPropertyStatus(String status);

    void deleteByPropertyId(UUID propertyId);

    @Query("SELECT sd FROM SearchDocument sd WHERE sd.propertyId NOT IN " +
            "(SELECT p.id FROM com.roomwallah.property.domain.entity.Property p " +
            "WHERE p.deleted = false AND p.status = 'ACTIVE')")
    List<SearchDocument> findOrphaned();

    @Query("SELECT sd.city, COUNT(sd) FROM SearchDocument sd GROUP BY sd.city ORDER BY COUNT(sd) DESC")
    List<Object[]> countByCity();

    @Query(value = "SELECT DISTINCT sd.title FROM search_documents sd " +
            "WHERE LOWER(sd.title) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "AND (:city IS NULL OR sd.city = :city) " +
            "LIMIT :limit",
            nativeQuery = true)
    List<String> findAutoCompleteSuggestions(@Param("prefix") String prefix,
                                            @Param("city") String city,
                                            @Param("limit") int limit);

    @Query(value = "SELECT DISTINCT sd.city FROM search_documents sd " +
            "WHERE LOWER(sd.city) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "LIMIT :limit", nativeQuery = true)
    List<String> findMatchingCities(@Param("prefix") String prefix, @Param("limit") int limit);

    @Query(value = "SELECT DISTINCT sd.locality FROM search_documents sd " +
            "WHERE LOWER(sd.locality) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "AND (:city IS NULL OR sd.city = :city) " +
            "LIMIT :limit", nativeQuery = true)
    List<String> findMatchingLocalities(@Param("prefix") String prefix, @Param("city") String city, @Param("limit") int limit);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SearchDocument sd SET sd.viewCount = sd.viewCount + 1 WHERE sd.propertyId = :propertyId")
    void incrementViewCount(@Param("propertyId") UUID propertyId);
}
