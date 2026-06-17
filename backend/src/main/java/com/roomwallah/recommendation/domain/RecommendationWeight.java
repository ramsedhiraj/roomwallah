package com.roomwallah.recommendation.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "recommendation_weights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationWeight extends BaseEntity {

    @Column(name = "budget_weight", nullable = false)
    private double budgetWeight = 0.3;

    @Column(name = "proximity_weight", nullable = false)
    private double proximityWeight = 0.4;

    @Column(name = "recency_weight", nullable = false)
    private double recencyWeight = 0.15;

    @Column(name = "popularity_weight", nullable = false)
    private double popularityWeight = 0.15;
}
