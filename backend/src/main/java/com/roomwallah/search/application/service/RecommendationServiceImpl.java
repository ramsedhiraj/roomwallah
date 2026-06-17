package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.port.RecommendationEnginePort;
import com.roomwallah.search.domain.port.RecommendationEnginePort.RecommendationItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationEnginePort recommendationEnginePort;

    @Override
    @Cacheable(value = "recommendations", key = "#userId", cacheManager = "redisCacheManager", unless = "#result == null || #result.isEmpty()")
    public List<RecommendationItem> getRecommendations(UUID userId, int limit) {
        log.info("Generating recommendations for userId: {}, limit: {}", userId, limit);
        return recommendationEnginePort.recommend(userId, limit);
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true, cacheManager = "redisCacheManager")
    public void refreshRecommendationsCache() {
        log.info("Evicting all recommendation caches");
    }
}
