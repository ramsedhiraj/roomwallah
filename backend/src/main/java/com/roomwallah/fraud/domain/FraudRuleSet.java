package com.roomwallah.fraud.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fraud_rule_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleSet extends BaseEntity {

    @Column(name = "version_name", nullable = false, unique = true)
    private String versionName;

    @Column(name = "velocity_limit", nullable = false)
    private int velocityLimit = 3;

    @Column(name = "large_transaction_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal largeTransactionLimit = BigDecimal.valueOf(100000.00);

    @Column(nullable = false)
    private boolean active = true;
}
