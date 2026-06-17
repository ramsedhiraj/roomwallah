package com.roomwallah.search.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.search.application.service.SearchIndexService;
import com.roomwallah.search.application.service.TrendingSearchService;
import com.roomwallah.search.domain.port.SearchAnalyticsPort;
import com.roomwallah.search.domain.repository.SavedSearchRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchJobs {

    private final TrendingSearchService trendingSearchService;
    private final SearchAnalyticsPort searchAnalyticsPort;
    private final SearchIndexService searchIndexService;
    private final SavedSearchRepository savedSearchRepository;
    private final SearchDocumentRepository searchDocumentRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private NotificationPort notificationPort;

    @Value("${roomwallah.search.analytics.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void aggregateTrendingQueries() {
        log.info("Running scheduled aggregation of trending search queries...");
        try {
            trendingSearchService.aggregateTrendingQueries();
        } catch (Exception e) {
            log.error("Failed to aggregate trending queries: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void cleanupAnalytics() {
        log.info("Running scheduled cleanup of search analytics log history...");
        try {
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            searchAnalyticsPort.deleteOlderThan(cutoff);
            log.info("Search analytics logs older than {} days cleaned up.", retentionDays);
        } catch (Exception e) {
            log.error("Failed to clean up old search analytics: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void reconcileIndexDrift() {
        log.info("Running scheduled reconciliation of search index drift...");
        try {
            searchIndexService.reconcileDrift();
        } catch (Exception e) {
            log.error("Failed to reconcile search index drift: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 */4 * * *") // Every 4 hours
    @CacheEvict(value = "recommendations", allEntries = true)
    public void refreshRecommendationsCache() {
        log.info("Recommendations cache evicted by scheduled task to enforce fresh data.");
    }

    @Scheduled(cron = "0 0 1 * * *") // Daily at 1:00 AM
    public void processDailyDigests() {
        log.info("Running scheduled processing of daily saved search digests...");
        processDigests("DAILY", 1);
    }

    @Scheduled(cron = "0 0 1 * * 0") // Weekly on Sunday at 1:00 AM
    public void processWeeklyDigests() {
        log.info("Running scheduled processing of weekly saved search digests...");
        processDigests("WEEKLY", 7);
    }

    private void processDigests(String frequency, int daysAgo) {
        try {
            Instant since = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
            List<com.roomwallah.search.domain.entity.SearchDocument> newDocs = searchDocumentRepository.findAll().stream()
                    .filter(sd -> "ACTIVE".equalsIgnoreCase(sd.getPropertyStatus()))
                    .filter(sd -> sd.getCreatedAt() != null && sd.getCreatedAt().isAfter(since))
                    .toList();

            if (newDocs.isEmpty()) {
                log.info("No new active properties created in the last {} days. Skipping {} digest processing.", daysAgo, frequency);
                return;
            }

            List<com.roomwallah.search.domain.entity.SavedSearch> savedSearches = savedSearchRepository.findByNotificationEnabledTrue();
            for (var saved : savedSearches) {
                if (frequency.equalsIgnoreCase(saved.getNotificationFrequency())) {
                    try {
                        com.roomwallah.search.domain.model.SearchQuery query = objectMapper.readValue(saved.getSerializedQuery(), com.roomwallah.search.domain.model.SearchQuery.class);
                        List<com.roomwallah.search.domain.entity.SearchDocument> matches = new ArrayList<>();
                        for (var doc : newDocs) {
                            if (matchesQuery(doc, query)) {
                                matches.add(doc);
                            }
                        }

                        if (!matches.isEmpty()) {
                            com.roomwallah.user.entity.User user = entityManager.find(com.roomwallah.user.entity.User.class, saved.getUserId());
                            if (user != null && user.getEmail() != null) {
                                String subject = String.format("RoomWallah %s Digest: %d New Matches found!", 
                                        frequency.substring(0, 1).toUpperCase() + frequency.substring(1).toLowerCase(), matches.size());
                                StringBuilder body = new StringBuilder(String.format("Hello %s,\n\nHere are the new matching properties in the last %d days:\n\n", user.getFullName(), daysAgo));
                                for (var match : matches) {
                                    body.append(String.format("- %s in %s for %s\n  Link: /properties/%s\n\n", 
                                            match.getTitle(), match.getCity(), match.getPrice(), match.getSlug()));
                                }
                                body.append("Best regards,\nRoomWallah Team");

                                if (notificationPort != null) {
                                    notificationPort.sendEmail(user.getEmail(), subject, body.toString());
                                    log.info("Sent {} digest email to {} for saved search {}", frequency, user.getEmail(), saved.getId());
                                } else {
                                    log.info("[Mock {} Digest Email] To: {}, Subject: {}, Body: {}", frequency, user.getEmail(), subject, body.toString());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process {} digest for saved search: {}", frequency, saved.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to run {} digest job: {}", frequency, e.getMessage(), e);
        }
    }

    private boolean matchesQuery(com.roomwallah.search.domain.entity.SearchDocument doc, com.roomwallah.search.domain.model.SearchQuery query) {
        if (query.getText() != null && !query.getText().isBlank()) {
            String q = query.getText().toLowerCase();
            boolean titleMatch = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(q);
            boolean descMatch = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(q);
            if (!titleMatch && !descMatch) {
                return false;
            }
        }
        com.roomwallah.search.domain.model.SearchFilter filter = query.getFilter();
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
            java.math.BigDecimal price = doc.getPrice();
            java.math.BigDecimal min = filter.getPriceRange().getMinPrice();
            java.math.BigDecimal max = filter.getPriceRange().getMaxPrice();
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
}
