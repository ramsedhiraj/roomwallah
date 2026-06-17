package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.entity.FraudSignal;
import com.roomwallah.trust.domain.port.FraudDetectionPort;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class StubFraudDetectionAdapter implements FraudDetectionPort {
    @Override
    public List<FraudSignal> detectFraud(UUID userId) {
        List<FraudSignal> signals = new ArrayList<>();
        signals.add(FraudSignal.builder()
                .userId(userId)
                .fraudType("SUSPICIOUS_IP_ROTATION")
                .severity("LOW")
                .metadataJson("{\"ips\":[\"192.168.1.1\",\"10.0.0.1\"]}")
                .detectedAt(Instant.now())
                .signalType("SUSPICIOUS_IP_ROTATION")
                .description("Suspicious IP rotation detected")
                .brokerRiskScore(10)
                .build());
        return signals;
    }
}
