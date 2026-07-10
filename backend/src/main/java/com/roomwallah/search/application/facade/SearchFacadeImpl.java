package com.roomwallah.search.application.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.search.application.service.*;
import com.roomwallah.search.domain.entity.SavedSearch;
import com.roomwallah.search.domain.entity.TrendingQuery;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.GeoSearchPort;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;
import com.roomwallah.search.domain.repository.SavedSearchRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.search.domain.repository.TrendingQueryRepository;
import com.roomwallah.search.domain.repository.SearchAnalyticsRepository;
import com.roomwallah.search.infrastructure.adapter.SearchEngineRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchFacadeImpl implements SearchFacade {

    private final PropertySearchService propertySearchService;
    private final AutoCompleteService autoCompleteService;
    private final TrendingSearchService trendingSearchService;
    private final SavedSearchService savedSearchService;
    private final SearchIndexService searchIndexService;
    private final SearchEngineRegistry searchEngineRegistry;
    private final SearchDocumentRepository searchDocumentRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final TrendingQueryRepository trendingQueryRepository;
    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final GeoSearchPort geoSearchPort;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final Map<String, AtomicLong> telemetryStats = new ConcurrentHashMap<>();

    @Override
    public SearchResult search(SearchQuery query, UUID userId, String correlationId) {
        return propertySearchService.search(query, userId, correlationId);
    }

    @Override
    public List<String> autoComplete(String prefix, String city, int limit) {
        return autoCompleteService.suggest(prefix, city, limit);
    }

    @Override
    public List<TrendingQuery> getTrending(String city, int limit) {
        return trendingSearchService.getTrending(city, limit);
    }

    @Override
    public SavedSearch createSavedSearch(UUID userId, String serializedQuery, boolean notificationEnabled) {
        return savedSearchService.create(userId, serializedQuery, notificationEnabled);
    }

    @Override
    public List<SavedSearch> getSavedSearches(UUID userId) {
        return savedSearchService.getByUser(userId);
    }

    @Override
    public void deleteSavedSearch(UUID id, UUID userId) {
        savedSearchService.delete(id, userId);
    }

    @Override
    public long reindexAll() {
        searchIndexService.triggerFullReindexing();
        return searchDocumentRepository.countByPropertyStatus("ACTIVE");
    }

    @Override
    public long reindexIncremental(Instant since) {
        searchIndexService.triggerIncrementalReindexing(since);
        return searchDocumentRepository.countByPropertyStatus("ACTIVE");
    }

    @Override
    public long reconcile() {
        long before = searchDocumentRepository.countByPropertyStatus("ACTIVE");
        searchIndexService.reconcileDrift();
        long after = searchDocumentRepository.countByPropertyStatus("ACTIVE");
        return Math.abs(after - before);
    }

    @Override
    public Map<String, Object> getIndexHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            var activeEngine = searchEngineRegistry.resolve();
            health.put("status", "UP");
            health.put("provider", activeEngine.providerName());
            health.put("postgis", geoSearchPort.isPostGisAvailable());
            health.put("availableEngines", List.of("postgresql", "elasticsearch"));
        } catch (Exception e) {
            log.error("Search health check failed: {}", e.getMessage());
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    @PostConstruct
    public void initGauges() {
        if (meterRegistry != null) {
            meterRegistry.gauge("search.zero_results.count", this,
                facade -> {
                    try {
                        return facade.searchAnalyticsRepository.findAll().stream()
                            .filter(a -> a.getResultCount() == 0)
                            .count();
                    } catch (Exception e) {
                        return 0.0;
                    }
                });
            meterRegistry.gauge("search.latency.average", this,
                facade -> {
                    try {
                        return facade.searchAnalyticsRepository.findAll().stream()
                            .mapToLong(com.roomwallah.search.domain.entity.SearchAnalytics::getExecutionTimeMs)
                            .average()
                            .orElse(0.0);
                    } catch (Exception e) {
                        return 0.0;
                    }
                });
            meterRegistry.gauge("search.cache.hit_ratio", this,
                facade -> {
                    try {
                        List<com.roomwallah.search.domain.entity.SearchAnalytics> all = facade.searchAnalyticsRepository.findAll();
                        if (all.isEmpty()) return 0.0;
                        long hits = all.stream().filter(com.roomwallah.search.domain.entity.SearchAnalytics::isCacheHit).count();
                        return (double) hits / all.size();
                    } catch (Exception e) {
                        return 0.0;
                    }
                });
            meterRegistry.gauge("search.pwa.installs", telemetryStats, map -> map.computeIfAbsent("pwa.installs", k -> new AtomicLong(0)).get());
            meterRegistry.gauge("search.offline.sessions", telemetryStats, map -> map.computeIfAbsent("offline.sessions", k -> new AtomicLong(0)).get());
            meterRegistry.gauge("search.autocomplete.requests", telemetryStats, map -> map.computeIfAbsent("autocomplete.requests", k -> new AtomicLong(0)).get());
            meterRegistry.gauge("search.recommendation.clicks", telemetryStats, map -> map.computeIfAbsent("recommendation.clicks", k -> new AtomicLong(0)).get());
            meterRegistry.gauge("search.canary.bucket_allocation", telemetryStats, map -> map.computeIfAbsent("canary.bucket_allocation", k -> new AtomicLong(0)).get());
        }
    }

    @Override
    public Map<String, Object> getIndexStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long activeDocs = searchDocumentRepository.countByPropertyStatus("ACTIVE");
            long totalSavedSearches = savedSearchRepository.count();
            long totalTrending = trendingQueryRepository.count();

            stats.put("activeDocuments", activeDocs);
            stats.put("totalSavedSearches", totalSavedSearches);
            stats.put("totalTrendingQueries", totalTrending);

            List<Object[]> cityCounts = searchDocumentRepository.countByCity();
            Map<String, Long> cityBreakdown = cityCounts.stream()
                    .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> ((Number) row[1]).longValue(),
                            (v1, v2) -> v1
                    ));
            stats.put("cityBreakdown", cityBreakdown);

            // Admin Analytics
            List<com.roomwallah.search.domain.entity.SearchAnalytics> recentAnalytics = searchAnalyticsRepository.findAll();
            long zeroResults = recentAnalytics.stream().filter(a -> a.getResultCount() == 0).count();
            long slowSearches = recentAnalytics.stream().filter(a -> a.getExecutionTimeMs() > 500).count();
            
            Map<String, Long> popularFilters = new HashMap<>();
            for (var a : recentAnalytics) {
                if (a.getFiltersJson() != null && !a.getFiltersJson().isBlank()) {
                    try {
                        Map<?, ?> filterMap = objectMapper.readValue(a.getFiltersJson(), Map.class);
                        for (Object key : filterMap.keySet()) {
                            popularFilters.merge(key.toString(), 1L, Long::sum);
                        }
                    } catch (Exception ignored) {}
                }
            }

            stats.put("zeroResultSearchCount", zeroResults);
            stats.put("slowSearchCount", slowSearches);
            stats.put("popularFilters", popularFilters);
            stats.put("recommendationCallsCount", telemetryStats.computeIfAbsent("recommendation.calls", k -> new AtomicLong(0)).get());

        } catch (Exception e) {
            log.error("Failed to retrieve search statistics: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    @Override
    public void executeMaintenanceTasks() {
        log.info("Executing administrative search maintenance tasks");
        try {
            searchIndexService.reconcileDrift();
            log.info("Administrative search maintenance completed successfully");
        } catch (Exception e) {
            log.error("Error executing maintenance tasks: {}", e.getMessage(), e);
        }
    }

    @Override
    public void trackTelemetry(String eventName) {
        if (eventName != null) {
            telemetryStats.computeIfAbsent(eventName, k -> new AtomicLong(0)).incrementAndGet();
            if (meterRegistry != null) {
                meterRegistry.counter("search.telemetry." + eventName).increment();
            }
        }
    }
}
