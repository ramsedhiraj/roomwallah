package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.FraudSignal;
import java.util.List;
import java.util.UUID;

public interface FraudDetectionPort {
    List<FraudSignal> detectFraud(UUID userId);
}
