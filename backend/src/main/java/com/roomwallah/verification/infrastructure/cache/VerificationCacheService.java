package com.roomwallah.verification.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCacheService {

    private final CacheManager cacheManager;

    public void evictTrustScore(UUID userId) {
        evict("trust-score", userId);
    }

    public void evictBadges(UUID userId) {
        evict("badges", userId);
    }

    public void evictVerification(UUID userId) {
        evict("verifications", userId);
    }

    public void evictAll(UUID userId) {
        evictTrustScore(userId);
        evictBadges(userId);
        evictVerification(userId);
    }

    private void evict(String cacheName, UUID userId) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(userId);
                log.info("Evicted key {} from cache: {}", userId, cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to evict key from cache {}: {}", cacheName, e.getMessage());
        }
    }
}
