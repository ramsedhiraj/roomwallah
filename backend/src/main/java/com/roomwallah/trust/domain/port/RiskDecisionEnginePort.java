package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.valueobject.RiskAssessment;
import java.util.UUID;

public interface RiskDecisionEnginePort {
    RiskAssessment assess(UUID userId);
}
