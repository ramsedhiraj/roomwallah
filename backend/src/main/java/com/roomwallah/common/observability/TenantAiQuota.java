package com.roomwallah.common.observability;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tenant_ai_quotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAiQuota {

    @Id
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "monthly_limit_usd", nullable = false, precision = 15, scale = 4)
    private BigDecimal monthlyLimitUsd;

    @Column(name = "current_spend_usd", nullable = false, precision = 15, scale = 4)
    private BigDecimal currentSpendUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
