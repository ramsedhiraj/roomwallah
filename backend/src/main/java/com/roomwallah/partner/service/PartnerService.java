package com.roomwallah.partner.service;

import com.roomwallah.common.cache.MultiLevelCacheService;
import com.roomwallah.notification.service.NotificationService;
import com.roomwallah.partner.domain.PartnerApiKey;
import com.roomwallah.partner.repository.PartnerApiKeyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final MultiLevelCacheService multiLevelCacheService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateSecureKey() {
        byte[] bytes = new byte[32]; // 32 bytes = 256 bits entropy
        SECURE_RANDOM.nextBytes(bytes);
        return "rw_key_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public String createApiKey(String partnerName, String scopes, int dailyQuota) {
        String rawKey = generateSecureKey();
        String hashed = sha256(rawKey);

        PartnerApiKey partnerKey = PartnerApiKey.builder()
                .partnerName(partnerName)
                .apiKeyHash(hashed)
                .scopes(scopes)
                .enabled(true)
                .revoked(false)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS)) // set to 30 days for easy expiry testing
                .dailyQuotaLimit(dailyQuota)
                .currentDailyUsage(0)
                .quotaResetAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .rotationHistory("Created key on " + Instant.now())
                .build();
        partnerKey.setVersion(0L);

        partnerApiKeyRepository.save(partnerKey);
        log.info("Created new partner API key for partner: {}", partnerName);
        return rawKey;
    }

    @Transactional
    public PartnerApiKey getApiKeyByHashCached(String hash) {
        String cacheKey = "key:" + hash;
        return multiLevelCacheService.get("developerKeys", cacheKey, PartnerApiKey.class, () -> 
            partnerApiKeyRepository.findByApiKeyHash(hash).orElse(null)
        );
    }

    @Transactional
    public ApiKeyValidationResult validateApiKey(String rawKey) {
        String hash = sha256(rawKey);
        PartnerApiKey key = getApiKeyByHashCached(hash);

        if (key == null || !key.isEnabled() || key.isRevoked()) {
            return ApiKeyValidationResult.INVALID;
        }

        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            return ApiKeyValidationResult.EXPIRED;
        }

        Instant now = Instant.now();
        if (now.isAfter(key.getQuotaResetAt())) {
            key.setCurrentDailyUsage(0);
            key.setQuotaResetAt(now.plus(1, ChronoUnit.DAYS));
        }

        if (key.getCurrentDailyUsage() >= key.getDailyQuotaLimit()) {
            log.warn("Partner API Key for partner '{}' exceeded daily quota limit: {}", key.getPartnerName(), key.getDailyQuotaLimit());
            return ApiKeyValidationResult.QUOTA_EXCEEDED;
        }

        key.setCurrentDailyUsage(key.getCurrentDailyUsage() + 1);
        key.setLastUsedAt(now);
        partnerApiKeyRepository.save(key);

        multiLevelCacheService.evict("developerKeys", "key:" + hash);

        return ApiKeyValidationResult.VALID;
    }

    @Transactional
    public void revokeKey(UUID id) {
        partnerApiKeyRepository.findById(id).ifPresent(key -> {
            key.setRevoked(true);
            key.setEnabled(false);
            key.setRotationHistory(key.getRotationHistory() + "; Revoked on " + Instant.now());
            partnerApiKeyRepository.save(key);
            multiLevelCacheService.evict("developerKeys", "key:" + key.getApiKeyHash());
            log.info("Revoked partner API key ID: {}", id);
        });
    }

    @Transactional
    public String rotateKey(UUID id) {
        PartnerApiKey oldKey = partnerApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        oldKey.setRevoked(true);
        oldKey.setEnabled(false);
        oldKey.setRotationHistory(oldKey.getRotationHistory() + "; Rotated on " + Instant.now());
        partnerApiKeyRepository.save(oldKey);
        multiLevelCacheService.evict("developerKeys", "key:" + oldKey.getApiKeyHash());

        String newRawKey = generateSecureKey();
        String hashed = sha256(newRawKey);

        PartnerApiKey newKey = PartnerApiKey.builder()
                .partnerName(oldKey.getPartnerName())
                .apiKeyHash(hashed)
                .scopes(oldKey.getScopes())
                .enabled(true)
                .revoked(false)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .dailyQuotaLimit(oldKey.getDailyQuotaLimit())
                .currentDailyUsage(0)
                .quotaResetAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .rotationHistory("Rotated from Key ID " + id + " on " + Instant.now())
                .build();
        newKey.setVersion(0L);

        partnerApiKeyRepository.save(newKey);
        log.info("Rotated partner API key ID: {} to new key", id);
        return newRawKey;
    }

    @Transactional
    public void checkKeyExpirations() {
        log.info("Checking developer API keys expiration...");
        Instant warnThreshold = Instant.now().plus(7, ChronoUnit.DAYS); // Warn if expiring in next 7 days

        List<PartnerApiKey> expiringKeys = partnerApiKeyRepository.findAll().stream()
                .filter(k -> k.isEnabled() && !k.isRevoked())
                .filter(k -> k.getExpiresAt() != null && k.getExpiresAt().isBefore(warnThreshold))
                .filter(k -> k.getRotationRemindedAt() == null || k.getRotationRemindedAt().isBefore(Instant.now().minus(1, ChronoUnit.DAYS)))
                .toList();

        if (expiringKeys.isEmpty()) {
            return;
        }

        // Find an admin user to notify
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN && !u.isDeleted())
                .findFirst()
                .orElse(null);

        if (admin == null) {
            log.warn("No admin user found to receive API key expiration warnings.");
            return;
        }

        for (PartnerApiKey key : expiringKeys) {
            String warningMsg = String.format("Partner API key for '%s' is set to expire on %s. Please rotate the key to prevent disruption.",
                    key.getPartnerName(), key.getExpiresAt());

            notificationService.sendNotification(admin.getId(), "API Key Expiry Warning", warningMsg, "SYSTEM");
            key.setRotationRemindedAt(Instant.now());
            partnerApiKeyRepository.save(key);
            log.warn("Sent expiration warning for partner key: {}", key.getPartnerName());
        }
    }
}
