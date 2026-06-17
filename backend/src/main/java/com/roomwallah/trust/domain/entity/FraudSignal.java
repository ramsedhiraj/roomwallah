package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_signals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudSignal extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fraud_type", nullable = false, length = 100)
    private String fraudType;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    // Backward compatibility fields
    @Column(name = "signal_type", length = 100)
    private String signalType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "broker_risk_score")
    private Integer brokerRiskScore;
}
