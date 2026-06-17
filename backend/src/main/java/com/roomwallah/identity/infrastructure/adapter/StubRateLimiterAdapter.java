package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.RateLimiterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StubRateLimiterAdapter implements RateLimiterPort {

    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean isBlocked(String key) {
        Integer val = attempts.get(key);
        boolean blocked = val != null && val >= 5;
        if (blocked) {
            log.warn("RateLimiterPort - Key [{}] is currently blocked.", key);
        }
        return blocked;
    }

    @Override
    public void recordFailedAttempt(String key) {
        attempts.merge(key, 1, Integer::sum);
        log.info("RateLimiterPort - Recorded failed login for [{}]. Attempts: {}", key, attempts.get(key));
    }

    @Override
    public void resetAttempts(String key) {
        attempts.remove(key);
        log.info("RateLimiterPort - Reset failed attempts for [{}].", key);
    }
}
