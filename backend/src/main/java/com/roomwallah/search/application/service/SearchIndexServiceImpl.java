package com.roomwallah.search.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.repository.SavedSearchRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.verification.domain.entity.TrustScore;
import com.roomwallah.verification.domain.entity.VerificationBadge;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexServiceImpl implements SearchIndexService {

    private final PropertyRepository propertyRepository;
    private final SearchDocumentRepository searchDocumentRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private NotificationPort notificationPort;

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void indexProperty(UUID propertyId) {
        log.info("Indexing property: {}", propertyId);
        updateIndex(propertyId, Instant.now());
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void reindexProperty(UUID propertyId) {
        log.info("Reindexing property: {}", propertyId);
        updateIndex(propertyId, Instant.now());
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void removeProperty(UUID propertyId) {
        log.info("Removing property from index: {}", propertyId);
        searchDocumentRepository.deleteByPropertyId(propertyId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void updateOwnerTrustScore(UUID ownerId, int trustScore) {
        log.info("Updating trust score for owner {} to {}", ownerId, trustScore);
        List<SearchDocument> docs = searchDocumentRepository.findAll().stream()
                .filter(sd -> ownerId.equals(sd.getOwnerId()))
                .toList();

        for (SearchDocument doc : docs) {
            doc.setTrustScore(trustScore);
            doc.setLastEventTimestamp(Instant.now());
            doc.setEventVersion(doc.getEventVersion() + 1);
            searchDocumentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void updateOwnerBadge(UUID ownerId, String badge) {
        log.info("Updating badge for owner {} to {}", ownerId, badge);
        List<SearchDocument> docs = searchDocumentRepository.findAll().stream()
                .filter(sd -> ownerId.equals(sd.getOwnerId()))
                .toList();

        for (SearchDocument doc : docs) {
            doc.setOwnerBadge(badge);
            doc.setLastEventTimestamp(Instant.now());
            doc.setEventVersion(doc.getEventVersion() + 1);
            searchDocumentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void triggerFullReindexing() {
        log.info("Triggering full reindexing of all properties...");
        List<Property> properties = propertyRepository.findAll();
        long count = 0;
        for (Property p : properties) {
            if (!p.isDeleted() && p.getStatus() == PropertyStatus.ACTIVE) {
                updateIndex(p.getId(), Instant.now());
                count++;
            } else {
                searchDocumentRepository.deleteByPropertyId(p.getId());
            }
        }
        log.info("Full reindexing completed. Indexed {} active properties.", count);
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void triggerIncrementalReindexing(Instant since) {
        log.info("Triggering incremental reindexing since: {}", since);
        List<Property> properties = propertyRepository.findAll().stream()
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(since))
                .toList();

        long count = 0;
        for (Property p : properties) {
            if (!p.isDeleted() && p.getStatus() == PropertyStatus.ACTIVE) {
                updateIndex(p.getId(), Instant.now());
                count++;
            } else {
                searchDocumentRepository.deleteByPropertyId(p.getId());
            }
        }
        log.info("Incremental reindexing completed. Processed {} modified properties.", count);
    }

    @Override
    @Transactional
    @CacheEvict(value = "search-results", allEntries = true)
    public void reconcileDrift() {
        log.info("Starting index drift reconciliation...");
        List<Property> allProperties = propertyRepository.findAll();
        List<SearchDocument> allDocs = searchDocumentRepository.findAll();

        Map<UUID, Property> activePropertiesMap = new HashMap<>();
        for (Property p : allProperties) {
            if (!p.isDeleted() && p.getStatus() == PropertyStatus.ACTIVE) {
                activePropertiesMap.put(p.getId(), p);
            }
        }

        Map<UUID, SearchDocument> docsMap = new HashMap<>();
        for (SearchDocument sd : allDocs) {
            docsMap.put(sd.getPropertyId(), sd);
        }

        long repairedCount = 0;

        // 1. Identify missing or mismatched documents
        for (Property p : activePropertiesMap.values()) {
            SearchDocument doc = docsMap.get(p.getId());
            if (doc == null) {
                log.info("Drift detected: Property {} is active but missing from index. Indexing it now.", p.getId());
                updateIndex(p.getId(), Instant.now());
                repairedCount++;
            } else if (hasMismatches(p, doc)) {
                log.info("Drift detected: Property {} is mismatched. Reindexing it now.", p.getId());
                updateIndex(p.getId(), Instant.now());
                repairedCount++;
            }
        }

        // 2. Identify orphaned or inactive documents
        for (SearchDocument doc : allDocs) {
            Property p = activePropertiesMap.get(doc.getPropertyId());
            if (p == null) {
                log.info("Drift detected: SearchDocument {} is orphaned/inactive. Removing from index.", doc.getPropertyId());
                searchDocumentRepository.deleteByPropertyId(doc.getPropertyId());
                repairedCount++;
            }
        }

        log.info("Index drift reconciliation completed. Repaired {} items.", repairedCount);
    }

    private boolean hasMismatches(Property p, SearchDocument doc) {
        BigDecimal propPrice = p.getPrice() != null ? p.getPrice().getAmount() : BigDecimal.ZERO;
        String propTitle = p.getTitle() != null ? p.getTitle() : "";
        String propStatus = p.getStatus() != null ? p.getStatus().name() : "";

        return propPrice.compareTo(doc.getPrice()) != 0 ||
                !propTitle.equals(doc.getTitle()) ||
                !propStatus.equals(doc.getPropertyStatus());
    }

    private void updateIndex(UUID propertyId, Instant eventTimestamp) {
        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            log.warn("Indexing failed: Property not found in repository for ID: {}", propertyId);
            searchDocumentRepository.deleteByPropertyId(propertyId);
            return;
        }
        if (property.isDeleted()) {
            log.warn("Indexing failed: Property is deleted for ID: {}", propertyId);
            searchDocumentRepository.deleteByPropertyId(propertyId);
            return;
        }
        if (property.getStatus() != PropertyStatus.ACTIVE) {
            log.warn("Indexing failed: Property is not active (status: {}) for ID: {}", property.getStatus(), propertyId);
            searchDocumentRepository.deleteByPropertyId(propertyId);
            return;
        }

        Optional<SearchDocument> existing = searchDocumentRepository.findByPropertyId(propertyId);
        if (existing.isPresent()) {
            SearchDocument existingDoc = existing.get();
            if (existingDoc.getLastEventTimestamp() != null && eventTimestamp != null &&
                    eventTimestamp.isBefore(existingDoc.getLastEventTimestamp())) {
                log.info("Skipping stale update for propertyId: {} due to out-of-order timestamp.", propertyId);
                return;
            }
        }

        SearchDocument doc = existing.orElse(new SearchDocument());
        doc.setPropertyId(property.getId());
        doc.setOwnerId(property.getOwnerId());
        doc.setListingRef(property.getListingRef());
        doc.setSlug(property.getSlug());
        doc.setTitle(property.getTitle());
        doc.setDescription(property.getDescription());
        doc.setCity(property.getAddress() != null ? property.getAddress().getCity() : "");
        doc.setLocality(property.getAddress() != null && property.getAddress().getLine2() != null ? property.getAddress().getLine2() : doc.getCity());
        doc.setState(property.getAddress() != null ? property.getAddress().getState() : null);
        doc.setCountry(property.getAddress() != null ? property.getAddress().getCountry() : null);
        doc.setPropertyType(property.getPropertyType().name());
        doc.setListingPurpose(property.getListingPurpose().name());
        doc.setPrice(property.getPrice() != null ? property.getPrice().getAmount() : BigDecimal.ZERO);
        doc.setBedrooms(property.getBedrooms());
        doc.setBathrooms(property.getBathrooms());
        doc.setParkingCount(property.getParkingCount());
        doc.setFurnishingStatus(property.getFurnishingStatus() != null ? property.getFurnishingStatus().name() : null);
        doc.setPetFriendly(property.isPetFriendly());
        doc.setPropertyStatus(property.getStatus().name());
        doc.setPublishedAt(property.getPublishedAt());
        doc.setCreatedAt(property.getCreatedAt() != null ? property.getCreatedAt() : Instant.now());
        doc.setUpdatedAt(property.getUpdatedAt() != null ? property.getUpdatedAt() : Instant.now());
        doc.setFacingDirection(property.getFacingDirection());
        doc.setAvailabilityDate(property.getAvailabilityDate());

        // Fetch owner details
        doc.setOwnerVerified(isOwnerVerified(property.getOwnerId()));
        doc.setTrustScore(getOwnerTrustScore(property.getOwnerId()));
        doc.setOwnerBadge(getOwnerBadge(property.getOwnerId()));
        doc.setMediaCount(getMediaCount(propertyId));

        doc.setLastEventTimestamp(eventTimestamp != null ? eventTimestamp : Instant.now());
        doc.setEventVersion(doc.getEventVersion() + 1);
        log.info("Saving SearchDocument: propertyId={}, city={}, status={}", doc.getPropertyId(), doc.getCity(), doc.getPropertyStatus());
        searchDocumentRepository.saveAndFlush(doc);

        try {
            triggerInstantAlerts(doc);
        } catch (Exception e) {
            log.error("Failed to check/trigger instant alerts for property: {}", propertyId, e);
        }
    }

    private void triggerInstantAlerts(SearchDocument doc) {
        List<com.roomwallah.search.domain.entity.SavedSearch> savedSearches = savedSearchRepository.findByNotificationEnabledTrue();
        for (var saved : savedSearches) {
            if ("INSTANT".equalsIgnoreCase(saved.getNotificationFrequency())) {
                try {
                    SearchQuery query = objectMapper.readValue(saved.getSerializedQuery(), SearchQuery.class);
                    if (matchesQuery(doc, query)) {
                        User user = entityManager.find(User.class, saved.getUserId());
                        if (user != null && user.getEmail() != null) {
                            String subject = "RoomWallah Alert: New Property Match!";
                            String body = String.format("Hello %s,\n\nA new property matching your search was just listed: %s in %s for %s.\nCheck it out here: /properties/%s",
                                    user.getFullName(), doc.getTitle(), doc.getCity(), doc.getPrice(), doc.getSlug());
                            if (notificationPort != null) {
                                notificationPort.sendEmail(user.getEmail(), subject, body);
                                log.info("Sent instant alert email to {} for saved search {}", user.getEmail(), saved.getId());
                            } else {
                                log.info("[Mock Email] To: {}, Subject: {}, Body: {}", user.getEmail(), subject, body);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to process saved search match check for saved search: {}", saved.getId(), e);
                }
            }
        }
    }

    private boolean matchesQuery(SearchDocument doc, SearchQuery query) {
        if (query.getText() != null && !query.getText().isBlank()) {
            String q = query.getText().toLowerCase();
            boolean titleMatch = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(q);
            boolean descMatch = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(q);
            if (!titleMatch && !descMatch) {
                return false;
            }
        }
        SearchFilter filter = query.getFilter();
        if (filter == null) {
            return true;
        }
        if (filter.getCity() != null && !filter.getCity().isBlank() && !filter.getCity().equalsIgnoreCase(doc.getCity())) {
            return false;
        }
        if (filter.getLocality() != null && !filter.getLocality().isBlank() && !filter.getLocality().equalsIgnoreCase(doc.getLocality())) {
            return false;
        }
        if (filter.getPropertyType() != null && !filter.getPropertyType().isBlank() && !filter.getPropertyType().equalsIgnoreCase(doc.getPropertyType())) {
            return false;
        }
        if (filter.getListingPurpose() != null && !filter.getListingPurpose().isBlank() && !filter.getListingPurpose().equalsIgnoreCase(doc.getListingPurpose())) {
            return false;
        }
        if (filter.getPriceRange() != null) {
            BigDecimal price = doc.getPrice();
            BigDecimal min = filter.getPriceRange().getMinPrice();
            BigDecimal max = filter.getPriceRange().getMaxPrice();
            if (min != null && price.compareTo(min) < 0) return false;
            if (max != null && price.compareTo(max) > 0) return false;
        }
        if (filter.getBedrooms() != null && !filter.getBedrooms().equals(doc.getBedrooms())) {
            return false;
        }
        if (filter.getBathrooms() != null && !filter.getBathrooms().equals(doc.getBathrooms())) {
            return false;
        }
        if (filter.getPetFriendly() != null && filter.getPetFriendly() != doc.isPetFriendly()) {
            return false;
        }
        if (filter.getOwnerVerified() != null && filter.getOwnerVerified() != doc.isOwnerVerified()) {
            return false;
        }
        return true;
    }

    private boolean isOwnerVerified(UUID ownerId) {
        try {
            User user = entityManager.find(User.class, ownerId);
            return user != null && user.isIdentityVerified();
        } catch (Exception e) {
            log.warn("Failed to check owner verification for owner: {}, defaulting to false", ownerId);
            return false;
        }
    }

    private int getOwnerTrustScore(UUID ownerId) {
        try {
            List<TrustScore> list = entityManager.createQuery(
                    "SELECT ts FROM TrustScore ts WHERE ts.userId = :userId", TrustScore.class)
                    .setParameter("userId", ownerId)
                    .getResultList();
            if (!list.isEmpty()) {
                return list.get(0).getOverallScore();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch trust score for owner: {}, defaulting to 0", ownerId);
        }
        return 0;
    }

    private String getOwnerBadge(UUID ownerId) {
        try {
            List<VerificationBadge> list = entityManager.createQuery(
                    "SELECT vb FROM VerificationBadge vb WHERE vb.userId = :userId ORDER BY vb.awardedAt DESC", VerificationBadge.class)
                    .setParameter("userId", ownerId)
                    .getResultList();
            if (!list.isEmpty()) {
                return list.get(0).getBadgeLevel().name();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch badge for owner: {}, defaulting to null", ownerId);
        }
        return null;
    }

    private int getMediaCount(UUID propertyId) {
        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM property_media WHERE property_id = :propertyId")
                    .setParameter("propertyId", propertyId)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to fetch media count for property: {}, defaulting to 0", propertyId);
            return 0;
        }
    }
}
