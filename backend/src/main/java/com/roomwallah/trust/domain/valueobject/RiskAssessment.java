package com.roomwallah.trust.domain.valueobject;

import com.roomwallah.trust.domain.entity.RiskDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {
    private int trustScore;
    private int fraudScore;
    private int brokerScore;
    private RiskDecision decision;
    private String explanation;
}
