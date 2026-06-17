package com.roomwallah.recommendation.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringExplanation {
    private double proximityScore;
    private double budgetAffinityScore;
    private double recencyScore;
    private double popularityScore;
    private double totalScore;
    private String explanationDetails;
}
