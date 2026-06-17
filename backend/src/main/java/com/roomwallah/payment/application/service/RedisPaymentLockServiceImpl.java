package com.roomwallah.payment.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPaymentLockServiceImpl implements PaymentLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T executeWithLock(String lockKey, long leaseTimeSeconds, Supplier<T> task) {
        log.debug("Attempting to acquire lock for key: {}", lockKey);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(leaseTimeSeconds));
        
        if (acquired == null || !acquired) {
            log.warn("Failed to acquire lock for key: {}", lockKey);
            throw new IllegalStateException("Lock could not be acquired for key: " + lockKey + ". Operation is already in progress.");
        }

        try {
            return task.get();
        } finally {
            try {
                redisTemplate.delete(lockKey);
                log.debug("Released lock for key: {}", lockKey);
            } catch (Exception e) {
                log.error("Failed to release lock for key: {}", lockKey, e);
            }
        }
    }
}
