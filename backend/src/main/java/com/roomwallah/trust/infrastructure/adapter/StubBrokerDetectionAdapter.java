package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.entity.BrokerDetectionSignal;
import com.roomwallah.trust.domain.port.BrokerDetectionPort;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class StubBrokerDetectionAdapter implements BrokerDetectionPort {
    @Override
    public List<BrokerDetectionSignal> detect(UUID userId) {
        List<BrokerDetectionSignal> signals = new ArrayList<>();
        signals.add(BrokerDetectionSignal.builder()
                .userId(userId)
                .signalType("MULTIPLE_LISTINGS_SAME_PHONE")
                .signalWeight(new BigDecimal("0.1500"))
                .metadataJson("{\"phone_count\": 1}")
                .detectedAt(Instant.now())
                .build());
        return signals;
    }
}
