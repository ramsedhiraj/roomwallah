package com.roomwallah.featurestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "ml_features")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlFeature {

    @Id
    @Column(name = "feature_key", length = 100)
    private String featureKey;

    @Column(name = "feature_value", nullable = false)
    private String featureValue;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "feature_version", nullable = false)
    private int featureVersion;
}
