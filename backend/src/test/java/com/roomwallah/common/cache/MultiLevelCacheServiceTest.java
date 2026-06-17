package com.roomwallah.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class MultiLevelCacheServiceTest {

    @Mock
    private CacheManager caffeineCacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Cache caffeineCache;

    private MultiLevelCacheService cacheService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(caffeineCacheManager.getCache("test-cache")).thenReturn(caffeineCache);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new MultiLevelCacheService(caffeineCacheManager, redisTemplate);
    }

    @Test
    public void testGet_L1Hit() {
        when(caffeineCache.get(eq("key1"), eq(String.class))).thenReturn("val1");

        String res = cacheService.get("test-cache", "key1", String.class, () -> "db-val");

        assertEquals("val1", res);
        verify(caffeineCache, times(1)).get(eq("key1"), eq(String.class));
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    public void testGet_L1Miss_L2Hit() {
        when(caffeineCache.get(eq("key1"), eq(String.class))).thenReturn(null);
        when(valueOperations.get("test-cache:key1")).thenReturn("val2");

        String res = cacheService.get("test-cache", "key1", String.class, () -> "db-val");

        assertEquals("val2", res);
        verify(caffeineCache, times(1)).get(eq("key1"), eq(String.class));
        verify(valueOperations, times(1)).get("test-cache:key1");
        verify(caffeineCache, times(1)).put("key1", "val2");
    }

    @Test
    public void testGet_BothMiss_DbLoad() {
        when(caffeineCache.get(eq("key1"), eq(String.class))).thenReturn(null);
        when(valueOperations.get("test-cache:key1")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("lock:test-cache:key1"), eq("LOCKED"), any())).thenReturn(true);

        AtomicInteger dbLoads = new AtomicInteger(0);
        String res = cacheService.get("test-cache", "key1", String.class, () -> {
            dbLoads.incrementAndGet();
            return "db-val";
        });

        assertEquals("db-val", res);
        assertEquals(1, dbLoads.get());
        verify(caffeineCache, times(1)).put("key1", "db-val");
        verify(valueOperations, times(1)).set(eq("test-cache:key1"), eq("db-val"), any());
    }

    @Test
    public void testEvict() {
        cacheService.evict("test-cache", "key1");
        verify(caffeineCache, times(1)).evict("key1");
        verify(redisTemplate, times(1)).delete("test-cache:key1");
    }
}
