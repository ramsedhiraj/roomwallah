package com.roomwallah.search.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.search.domain.entity.SearchAnalytics;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.SearchAnalyticsPort;
import com.roomwallah.search.domain.port.SearchEnginePort;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;
import com.roomwallah.search.infrastructure.adapter.SearchEngineRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertySearchServiceImpl implements PropertySearchService {

    private final SearchEngineRegistry searchEngineRegistry;
    private final SearchAnalyticsPort searchAnalyticsPort;
    private final TrendingSearchService trendingSearchService;
    private final CacheManager caffeineCacheManager;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    // Concurrent request deduplication map
    private final Map<SearchQuery, CompletableFuture<SearchResult>> pendingSearches = new ConcurrentHashMap<>();

    private static final String REDIS_CACHE_PREFIX = "search:results:";

    @Override
    public SearchResult search(SearchQuery query, UUID userId, String correlationId) {
        Instant startTime = Instant.now();
        boolean cacheHit = false;
        SearchResult result = null;

        // 1. Check L1 Cache (Caffeine)
        Cache l1Cache = caffeineCacheManager.getCache("search-results");
        if (l1Cache != null) {
            Cache.ValueWrapper wrapper = l1Cache.get(query);
            if (wrapper != null) {
                result = (SearchResult) wrapper.get();
                cacheHit = true;
                incrementMetric("search.cache.hit", "level", "L1");
            }
        }

        // 2. Check L2 Cache (Redis)
        if (result == null) {
            String redisKey = REDIS_CACHE_PREFIX + query.hashCode();
            try {
                String cachedJson = redisTemplate.opsForValue().get(redisKey);
                if (cachedJson != null) {
                    result = deserializeSearchResult(cachedJson);
                    if (result != null) {
                        cacheHit = true;
                        incrementMetric("search.cache.hit", "level", "L2");
                        // Populate back to L1
                        if (l1Cache != null) {
                            l1Cache.put(query, result);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("L2 Redis cache retrieval failed, degrading to database: {}", e.getMessage());
            }
        }

        // 3. Request Deduplication & Stampede Protection
        if (result == null) {
            CompletableFuture<SearchResult> future = new CompletableFuture<>();
            CompletableFuture<SearchResult> existing = pendingSearches.putIfAbsent(query, future);
            if (existing != null) {
                log.info("Deduplicating concurrent search query. hash: {}", query.hashCode());
                result = existing.join();
            } else {
                try {
                    log.info("Cache miss for search query. Initiating execution. hash: {}", query.hashCode());
                    result = executeSearchAndCache(query);
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                    throw t;
                } finally {
                    pendingSearches.remove(query);
                }
            }
        }

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        recordSearchMetrics(durationMs, cacheHit);

        // 4. Async Search Analytics & Trending
        logSearchAnalytics(query, userId, correlationId, durationMs, result != null ? result.totalCount() : 0, cacheHit);
        recordTrendingText(query);

        return result;
    }

    // Fallback/Legacy interface implementation
    @Override
    public SearchResult search(SearchQuery query, UUID userId) {
        return search(query, userId, UUID.randomUUID().toString());
    }

    @Override
    public long count(SearchQuery query) {
        try {
            SearchEnginePort engine = searchEngineRegistry.resolve();
            return engine.count(query);
        } catch (Exception e) {
            log.error("Failed to execute count query: {}", e.getMessage(), e);
            return 0L;
        }
    }

    private SearchResult executeSearchAndCache(SearchQuery query) {
        SearchEnginePort engine = searchEngineRegistry.resolve();
        SearchResult searchResult = engine.search(query);

        // Populate L1 Cache
        Cache l1Cache = caffeineCacheManager.getCache("search-results");
        if (l1Cache != null && searchResult != null) {
            l1Cache.put(query, searchResult);
        }

        // Populate L2 Cache (Redis)
        if (searchResult != null) {
            String redisKey = REDIS_CACHE_PREFIX + query.hashCode();
            try {
                String json = serializeSearchResult(searchResult);
                redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("Failed to write search results to L2 Cache: {}", e.getMessage());
            }
        }

        return searchResult;
    }

    private void logSearchAnalytics(SearchQuery query, UUID userId, String correlationId, long executionTimeMs, long totalCount, boolean cacheHit) {
        CompletableFuture.runAsync(() -> {
            try {
                SearchAnalytics analytics = new SearchAnalytics();
                analytics.setId(UUID.randomUUID());
                analytics.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
                analytics.setUserId(userId);
                analytics.setSearchText(query.getText());
                analytics.setExecutionTimeMs(executionTimeMs);
                analytics.setResultCount((int) totalCount);
                analytics.setCacheHit(cacheHit);
                analytics.setCreatedAt(Instant.now());

                if (query.getFilter() != null) {
                    analytics.setFiltersJson(objectMapper.writeValueAsString(query.getFilter()));
                }

                searchAnalyticsPort.recordSearch(analytics);
            } catch (Exception e) {
                log.error("Failed to record search analytics: {}", e.getMessage());
            }
        });
    }

    private void recordTrendingText(SearchQuery query) {
        if (query.getText() != null && !query.getText().isBlank()) {
            String city = query.getFilter() != null ? query.getFilter().getCity() : null;
            CompletableFuture.runAsync(() -> {
                try {
                    trendingSearchService.recordSearchQuery(query.getText(), city);
                } catch (Exception e) {
                    log.error("Failed to record trending search: {}", e.getMessage());
                }
            });
        }
    }

    private String serializeSearchResult(SearchResult result) throws Exception {
        return objectMapper.writeValueAsString(result);
    }

    private SearchResult deserializeSearchResult(String json) {
        try {
            // Because SearchResult is a record with generic List, Jackson needs help or we deserialize fields.
            // Let's read tree and build SearchResult.
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            List<?> docList = (List<?>) map.get("documents");
            List<SearchDocument> documents = new ArrayList<>();
            if (docList != null) {
                for (Object o : docList) {
                    documents.add(objectMapper.convertValue(o, SearchDocument.class));
                }
            }
            String nextCursor = (String) map.get("nextCursor");
            long totalCount = ((Number) map.get("totalCount")).longValue();
            return new SearchResult(documents, nextCursor, totalCount);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached search result: {}", e.getMessage());
            return null;
        }
    }

    private void recordSearchMetrics(long latencyMs, boolean cacheHit) {
        if (meterRegistry != null) {
            meterRegistry.timer("search.latency").record(Duration.ofMillis(latencyMs));
            meterRegistry.counter("search.requests", "cache_hit", String.valueOf(cacheHit)).increment();
        }
    }

    private void incrementMetric(String name, String... tags) {
        if (meterRegistry != null) {
            meterRegistry.counter(name, tags).increment();
        }
    }
}
