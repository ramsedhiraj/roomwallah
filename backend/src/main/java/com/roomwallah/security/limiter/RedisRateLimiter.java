package com.roomwallah.security.limiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return false;
            }
            if (count == 1) {
                // First request, set expiration
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            log.debug("Rate limit key: {} Count: {}/{}", redisKey, count, maxRequests);
            return count <= maxRequests;
        } catch (Exception e) {
            log.error("Redis rate limit check failed: {}. Fallback to ALLOW.", e.getMessage());
            // Fail open in case Redis is down, or we could fail closed. Standard is fail open to prevent complete outage.
            return true;
        }
    }
}
