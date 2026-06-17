package com.roomwallah.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;

@Slf4j
@Service
public class MultiLevelCacheService {

    private final CacheManager caffeineCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Random random = new Random();

    public MultiLevelCacheService(
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Class<T> clazz, Supplier<T> databaseLoader) {
        // 1. Check L1 Cache (Caffeine)
        Cache l1 = caffeineCacheManager.getCache(cacheName);
        if (l1 != null) {
            try {
                T value = l1.get(key, clazz);
                if (value != null) {
                    log.debug("L1 Cache Hit for {}:{}", cacheName, key);
                    return value;
                }
            } catch (Exception e) {
                log.warn("L1 Cache read error for {}:{}", cacheName, key, e);
            }
        }

        // 2. Check L2 Cache (Redis)
        String redisKey = cacheName + ":" + key;
        T l2Value = null;
        try {
            l2Value = (T) redisTemplate.opsForValue().get(redisKey);
            if (l2Value != null) {
                log.debug("L2 Cache Hit for {}:{}", cacheName, key);
                if (l1 != null) {
                    l1.put(key, l2Value);
                }
                return l2Value;
            }
        } catch (Exception e) {
            log.warn("L2 Cache read error for {}:{}", cacheName, key, e);
        }

        // 3. Cache Miss: Rebuild with Distributed Lock
        String lockKey = "lock:" + cacheName + ":" + key;
        Boolean acquired = false;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));
        } catch (Exception e) {
            log.warn("Failed to acquire Redis lock for {}:{}", cacheName, key, e);
        }

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // Double check L2 cache under lock
                T checkValue = (T) redisTemplate.opsForValue().get(redisKey);
                if (checkValue != null) {
                    if (l1 != null) {
                        l1.put(key, checkValue);
                    }
                    return checkValue;
                }

                log.debug("Cache Miss with Lock for {}:{}. Loading from DB...", cacheName, key);
                T loaded = databaseLoader.get();
                if (loaded != null) {
                    // Save to L1
                    if (l1 != null) {
                        l1.put(key, loaded);
                    }
                    // Save to L2 with jittered TTL (+/- 10% on 600s base)
                    double jitterMultiplier = 0.9 + (random.nextDouble() * 0.2); // 0.9 to 1.1
                    long ttlSeconds = (long) (600 * jitterMultiplier);
                    redisTemplate.opsForValue().set(redisKey, loaded, Duration.ofSeconds(ttlSeconds));
                    log.debug("Saved to L2 with TTL: {} seconds", ttlSeconds);
                }
                return loaded;
            } finally {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception e) {
                    log.error("Failed to release Redis lock for {}:{}", cacheName, key, e);
                }
            }
        } else {
            // Lock held by another thread. Wait and attempt read once.
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            try {
                T checkValue = (T) redisTemplate.opsForValue().get(redisKey);
                if (checkValue != null) {
                    if (l1 != null) {
                        l1.put(key, checkValue);
                    }
                    return checkValue;
                }
            } catch (Exception e) {
                log.warn("L2 Cache read retry failed for {}:{}", cacheName, key, e);
            }

            // Fallback directly to DB
            return databaseLoader.get();
        }
    }

    public void evict(String cacheName, String key) {
        Cache l1 = caffeineCacheManager.getCache(cacheName);
        if (l1 != null) {
            l1.evict(key);
        }
        String redisKey = cacheName + ":" + key;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Failed to delete L2 key: {}", redisKey, e);
        }
        log.debug("Evicted cache for {}:{}", cacheName, key);
    }
}
