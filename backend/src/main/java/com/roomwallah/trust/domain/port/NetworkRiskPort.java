package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.valueobject.FraudEvidence;
import java.util.UUID;

public interface NetworkRiskPort {
    FraudEvidence analyzeNetworkRisk(UUID userId, String ipAddress, String userAgent);
}
