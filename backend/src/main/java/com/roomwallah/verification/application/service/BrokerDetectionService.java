package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.FraudSignal;
import java.util.List;
import java.util.UUID;

public interface BrokerDetectionService {
    void detectBrokerPatterns(UUID userId);
    List<FraudSignal> getFraudSignals(UUID userId);
    List<FraudSignal> getAllFraudSignals();
}
