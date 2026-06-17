package com.roomwallah.verification.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_scores")
@Getter
@Setter
public class TrustScore extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

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
