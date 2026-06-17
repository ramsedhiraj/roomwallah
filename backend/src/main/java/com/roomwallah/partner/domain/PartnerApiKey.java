package com.roomwallah.partner.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "partner_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerApiKey extends BaseEntity {

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(name = "api_key_hash", nullable = false, unique = true, length = 255)
    private String apiKeyHash;

    @Column(nullable = false, length = 255)
    private String scopes; // e.g. "read-only,booking,payment,analytics"

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "daily_quota_limit", nullable = false)
    private int dailyQuotaLimit = 1000;

    @Column(name = "current_daily_usage", nullable = false)
    private int currentDailyUsage = 0;

    @Column(name = "quota_reset_at", nullable = false)
    private Instant quotaResetAt;

    @Column(name = "rotation_history", columnDefinition = "TEXT")
    private String rotationHistory;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rotation_reminded_at")
    private Instant rotationRemindedAt;
}
