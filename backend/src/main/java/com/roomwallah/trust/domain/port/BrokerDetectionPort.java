package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.BrokerDetectionSignal;
import java.util.List;
import java.util.UUID;

public interface BrokerDetectionPort {
    List<BrokerDetectionSignal> detect(UUID userId);
}
