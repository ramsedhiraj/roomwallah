package com.roomwallah.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayProtectionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, Instant> localNonceCache = new ConcurrentHashMap<>();
    private static final long ALLOWED_WINDOW_SECONDS = 300; // 5 minutes

    public boolean validateNonce(String nonce, long timestampSeconds) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }

        long currentSeconds = Instant.now().getEpochSecond();
        if (Math.abs(currentSeconds - timestampSeconds) > ALLOWED_WINDOW_SECONDS) {
            log.warn("Nonce timestamp is outside the allowed window: {} vs current {}", timestampSeconds, currentSeconds);
            return false;
        }

        String redisKey = "nonce:" + nonce;
        try {
            Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "USED", Duration.ofSeconds(ALLOWED_WINDOW_SECONDS));
            if (isAbsent != null) {
                return isAbsent;
            }
        } catch (Exception e) {
            log.warn("Redis is unavailable for nonce checking. Falling back to local cache.", e);
        }

        // Fallback to local cache
        cleanLocalCache();
        if (localNonceCache.containsKey(nonce)) {
            return false;
        }
        localNonceCache.put(nonce, Instant.now());
        return true;
    }

    private void cleanLocalCache() {
        Instant cutoff = Instant.now().minusSeconds(ALLOWED_WINDOW_SECONDS);
        localNonceCache.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
