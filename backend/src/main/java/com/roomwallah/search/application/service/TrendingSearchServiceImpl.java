package com.roomwallah.search.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.search.domain.entity.TrendingQuery;
import com.roomwallah.search.domain.repository.TrendingQueryRepository;
import com.roomwallah.search.domain.entity.SearchAnalytics;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingSearchServiceImpl implements TrendingSearchService {

    private final TrendingQueryRepository trendingQueryRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    public void recordSearchQuery(String queryText, String city) {
        log.debug("Record search query for trending: text='{}', city='{}'", queryText, city);
    }

    @Override
    @Cacheable(value = "trending", key = "#city != null ? #city : 'global'", cacheManager = "redisCacheManager")
    @Transactional(readOnly = true)
    public List<TrendingQuery> getTrending(String city, int limit) {
        log.debug("Fetching trending searches for city: {}, limit: {}", city, limit);
        if (city != null && !city.isBlank()) {
            return trendingQueryRepository.findByCityOrderBySearchCountDesc(city).stream()
                    .limit(limit)
                    .toList();
        }
        return trendingQueryRepository.findTop20ByOrderBySearchCountDesc().stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "trending", allEntries = true)
    public void aggregateTrendingQueries() {
        log.info("Aggregating trending queries from search analytics of the last 24 hours...");
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        List<SearchAnalytics> analyticsList = entityManager.createQuery(
                "SELECT sa FROM SearchAnalytics sa WHERE sa.createdAt >= :since", SearchAnalytics.class)
                .setParameter("since", since)
                .getResultList();

        Map<QueryKey, Integer> counts = new HashMap<>();
        for (SearchAnalytics sa : analyticsList) {
            String queryText = sa.getSearchText();
            if (queryText == null || queryText.isBlank()) {
                continue;
            }
            String city = "Global";
            if (sa.getFiltersJson() != null && !sa.getFiltersJson().isBlank()) {
                try {
                    Map<?, ?> filters = objectMapper.readValue(sa.getFiltersJson(), Map.class);
                    if (filters.get("city") != null) {
                        city = filters.get("city").toString();
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse filters json for trending query count: {}", sa.getFiltersJson());
                }
            }
            QueryKey key = new QueryKey(queryText.trim().toLowerCase(), city);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        for (Map.Entry<QueryKey, Integer> entry : counts.entrySet()) {
            String queryText = entry.getKey().queryText();
            String city = entry.getKey().city();
            int count = entry.getValue();

            Optional<TrendingQuery> existing = trendingQueryRepository.findByQueryTextAndCity(queryText, city);
            if (existing.isPresent()) {
                TrendingQuery tq = existing.get();
                tq.setSearchCount(tq.getSearchCount() + count);
                tq.setLastAggregatedAt(Instant.now());
                trendingQueryRepository.save(tq);
            } else {
                TrendingQuery tq = new TrendingQuery();
                tq.setId(UUID.randomUUID());
                tq.setQueryText(queryText);
                tq.setCity(city);
                tq.setSearchCount(count);
                tq.setLastAggregatedAt(Instant.now());
                trendingQueryRepository.save(tq);
            }
        }
        log.info("Trending queries aggregation completed. Aggregated {} distinct query groups.", counts.size());
    }

    private record QueryKey(String queryText, String city) {}
}
