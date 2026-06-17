package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_score_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "previous_score", nullable = false)
    private int previousScore;

    @Column(name = "new_score", nullable = false)
    private int newScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "triggered_by_event")
    private String triggeredByEvent;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
