package com.roomwallah.fraud.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "risk_score", nullable = false)
    private BigDecimal riskScore;

    @Column(nullable = false, length = 50)
    private String status = "PENDING_REVIEW";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "rule_set_version")
    private String ruleSetVersion;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "escalated_to")
    private String escalatedTo;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
