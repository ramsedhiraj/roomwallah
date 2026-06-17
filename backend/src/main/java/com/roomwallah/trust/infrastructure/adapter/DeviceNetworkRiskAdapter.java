package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.port.NetworkRiskPort;
import com.roomwallah.trust.domain.valueobject.FraudEvidence;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DeviceNetworkRiskAdapter implements NetworkRiskPort {
    @Override
    public FraudEvidence analyzeNetworkRisk(UUID userId, String ipAddress, String userAgent) {
        String fingerprint = "fp_" + userId.toString().substring(0, 8) + "_" + (ipAddress != null ? ipAddress.hashCode() : "unknown");
        String context = "IP: " + ipAddress + ", User-Agent: " + userAgent + ", NetworkStatus: CLEAN";
        return new FraudEvidence(fingerprint, context);
    }
}
