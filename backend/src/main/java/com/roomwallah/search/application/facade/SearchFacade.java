package com.roomwallah.search.application.facade;

import com.roomwallah.search.domain.entity.SavedSearch;
import com.roomwallah.search.domain.entity.TrendingQuery;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.RecommendationEnginePort.RecommendationItem;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SearchFacade {

    SearchResult search(SearchQuery query, UUID userId, String correlationId);

    List<String> autoComplete(String prefix, String city, int limit);

    List<TrendingQuery> getTrending(String city, int limit);

    List<RecommendationItem> getRecommendations(UUID userId, int limit);

    SavedSearch createSavedSearch(UUID userId, String serializedQuery, boolean notificationEnabled);

    List<SavedSearch> getSavedSearches(UUID userId);

    void deleteSavedSearch(UUID id, UUID userId);

    long reindexAll();

    long reindexIncremental(Instant since);

    long reconcile();

    Map<String, Object> getIndexHealth();

    Map<String, Object> getIndexStats();

    void refreshRecommendationsCache();

    void executeMaintenanceTasks();

    void trackTelemetry(String eventName);
}
