package com.roomwallah.partner;

import com.roomwallah.common.cache.MultiLevelCacheService;
import com.roomwallah.notification.service.NotificationService;
import com.roomwallah.partner.domain.PartnerApiKey;
import com.roomwallah.partner.repository.PartnerApiKeyRepository;
import com.roomwallah.partner.service.ApiKeyValidationResult;
import com.roomwallah.partner.service.PartnerService;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PartnerServiceTest {

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private MultiLevelCacheService multiLevelCacheService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private PartnerService partnerService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        partnerService = new PartnerService(
                partnerApiKeyRepository, multiLevelCacheService, 
                userRepository, notificationService
        );
    }

    @Test
    public void testCreateApiKey_256BitsEntropy() {
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(i -> i.getArgument(0));

        String rawKey = partnerService.createApiKey("TestPartner", "read-only", 100);

        assertNotNull(rawKey);
        assertTrue(rawKey.startsWith("rw_key_"));
        // Key should have high entropy (uuid or 32 bytes URL encoded)
        assertTrue(rawKey.length() > 30);
    }

    @Test
    public void testValidateApiKey_QuotaLimits() {
        PartnerApiKey key = PartnerApiKey.builder()
                .partnerName("Test")
                .scopes("read-only")
                .enabled(true)
                .revoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .dailyQuotaLimit(2)
                .currentDailyUsage(1)
                .quotaResetAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        // Stub cache read for key lookup
        when(multiLevelCacheService.get(eq("developerKeys"), anyString(), eq(PartnerApiKey.class), any()))
                .thenReturn(key);

        // Validation 1: Passes and increments usage to 2
        ApiKeyValidationResult res1 = partnerService.validateApiKey("dummy-key");
        assertEquals(ApiKeyValidationResult.VALID, res1);
        assertEquals(2, key.getCurrentDailyUsage());

        // Validation 2: Fails with QUOTA_EXCEEDED
        ApiKeyValidationResult res2 = partnerService.validateApiKey("dummy-key");
        assertEquals(ApiKeyValidationResult.QUOTA_EXCEEDED, res2);
    }

    @Test
    public void testApiKeyExpirationWarnings() {
        PartnerApiKey key = PartnerApiKey.builder()
                .partnerName("Expiring Partner")
                .enabled(true)
                .revoked(false)
                .expiresAt(Instant.now().plus(2, ChronoUnit.DAYS)) // expiring soon
                .rotationRemindedAt(null)
                .build();

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        when(partnerApiKeyRepository.findAll()).thenReturn(List.of(key));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        partnerService.checkKeyExpirations();

        verify(notificationService, times(1)).sendNotification(eq(admin.getId()), eq("API Key Expiry Warning"), anyString(), eq("SYSTEM"));
        assertNotNull(key.getRotationRemindedAt());
    }
}
