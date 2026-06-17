package com.roomwallah;

import com.roomwallah.common.security.DeviceFingerprintService;
import com.roomwallah.common.security.ReplayProtectionService;
import com.roomwallah.common.security.SessionRiskEvaluator;
import com.roomwallah.common.security.ZeroTrustService;
import com.roomwallah.identity.infrastructure.provider.UserPrincipal;
import com.roomwallah.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ZeroTrustSecurityTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private UserPrincipal userPrincipal;
    @Mock
    private User user;

    private DeviceFingerprintService deviceFingerprintService;
    private ReplayProtectionService replayProtectionService;
    private SessionRiskEvaluator sessionRiskEvaluator;
    private ZeroTrustService zeroTrustService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);

        deviceFingerprintService = new DeviceFingerprintService();
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        replayProtectionService = new ReplayProtectionService(redisTemplate);
        
        sessionRiskEvaluator = new SessionRiskEvaluator();
        
        zeroTrustService = new ZeroTrustService(
                deviceFingerprintService,
                replayProtectionService,
                sessionRiskEvaluator
        );
    }

    @Test
    public void testReplayProtectionWithRedis() {
        String nonce = "test-nonce-123";
        long timestamp = Instant.now().getEpochSecond();

        // Simulate Redis is working and the key is absent
        when(valueOperations.setIfAbsent(eq("nonce:" + nonce), eq("USED"), any(Duration.class)))
                .thenReturn(true);

        boolean isValid = replayProtectionService.validateNonce(nonce, timestamp);
        assertTrue(isValid);

        // Simulate Redis returns false (nonce already used)
        when(valueOperations.setIfAbsent(eq("nonce:" + nonce), eq("USED"), any(Duration.class)))
                .thenReturn(false);

        boolean isReplayed = replayProtectionService.validateNonce(nonce, timestamp);
        assertFalse(isReplayed);
    }

    @Test
    public void testReplayProtectionRedisFallbackToLocalCache() {
        String nonce = "fallback-nonce-123";
        long timestamp = Instant.now().getEpochSecond();

        // Simulate Redis throws exception (unavailable)
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        // First call should succeed using local cache fallback
        boolean firstCall = replayProtectionService.validateNonce(nonce, timestamp);
        assertTrue(firstCall);

        // Second call with same nonce should fail (detected as replayed)
        boolean secondCall = replayProtectionService.validateNonce(nonce, timestamp);
        assertFalse(secondCall);
    }

    @Test
    public void testSessionRiskEvaluationTriggers() {
        UUID userId = UUID.randomUUID();
        String initialIp = "192.168.1.50";
        String initialUa = "Mozilla/5.0";
        String initialLoc = "New Delhi";

        // 1. Initial request establishes baseline
        var result = sessionRiskEvaluator.evaluateRisk(userId, initialIp, initialUa, initialLoc);
        assertEquals("LOW", result.getStatus());
        assertEquals(0.0, result.getScore());

        // 2. Change IP and User Agent to get score 0.7 (triggers MEDIUM)
        var mediumResult = sessionRiskEvaluator.evaluateRisk(userId, "192.168.1.100", "Mozilla/5.0 (New)", initialLoc);
        assertEquals("MEDIUM", mediumResult.getStatus());
        assertEquals(0.7, mediumResult.getScore(), 0.01);

        // 3. Change IP, User Agent, and Location within 60s to get score 1.0 (triggers HIGH)
        var travelResult = sessionRiskEvaluator.evaluateRisk(userId, "192.168.1.200", "Mozilla/5.0 (Hacker)", "Mumbai");
        assertEquals("HIGH", travelResult.getStatus());
        assertEquals(1.0, travelResult.getScore(), 0.01);
        assertTrue(travelResult.getTriggers().stream().anyMatch(t -> t.contains("impossible travel") || t.contains("velocity") || t.contains("changed")));
    }

    @Test
    public void testZeroTrustServiceHighRiskRejection() {
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(userPrincipal.user()).thenReturn(user);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(request.getHeader("X-Nonce")).thenReturn("nonce-abc");
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(Instant.now().getEpochSecond()));
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.50");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Device-Location")).thenReturn("Noida");
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);

        // Setup session history with baseline
        zeroTrustService.validateRequest(request);

        // Change IP, user agent, and location immediately to trigger HIGH risk
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.200");
        when(request.getHeader("User-Agent")).thenReturn("HackerAgent/1.0");
        when(request.getHeader("X-Device-Location")).thenReturn("Bangalore");

        assertThrows(SecurityException.class, () -> {
            zeroTrustService.validateRequest(request);
        });
    }
}
