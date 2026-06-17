package com.roomwallah.identity.domain.port;

public interface RateLimiterPort {
    boolean isBlocked(String key);
    void recordFailedAttempt(String key);
    void resetAttempts(String key);
}
