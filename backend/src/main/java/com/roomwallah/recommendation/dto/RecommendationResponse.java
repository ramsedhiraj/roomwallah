package com.roomwallah.recommendation.dto;

import com.roomwallah.property.domain.entity.Property;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private Property property;
    private ScoringExplanation explanation;
}
