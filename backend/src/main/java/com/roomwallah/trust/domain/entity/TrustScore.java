package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "TrustContextTrustScore")
@Table(name = "trust_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScore extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "current_score", nullable = false)
    private int currentScore;

    @Column(name = "score_version")
    private Integer scoreVersion;

    @Column(name = "rule_version")
    private String ruleVersion;

    @Column(name = "algorithm_version")
    private String algorithmVersion;

    @Column(name = "explanation_json", columnDefinition = "TEXT")
    private String explanationJson;

    // Backward compatibility fields for older components:
    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "identity_score", nullable = false)
    private int identityScore;

    @Column(name = "property_score", nullable = false)
    private int propertyScore;

    @Column(name = "review_score", nullable = false)
    private int reviewScore;

    @Column(name = "activity_score", nullable = false)
    private int activityScore;

    @Column(name = "fraud_penalty", nullable = false)
    private int fraudPenalty;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
