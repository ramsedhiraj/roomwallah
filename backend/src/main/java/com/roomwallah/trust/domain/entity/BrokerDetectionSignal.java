package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "broker_detection_signals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerDetectionSignal extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "signal_type", nullable = false, length = 100)
    private String signalType;

    @Column(name = "signal_weight", nullable = false, precision = 10, scale = 4)
    private BigDecimal signalWeight;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
}
