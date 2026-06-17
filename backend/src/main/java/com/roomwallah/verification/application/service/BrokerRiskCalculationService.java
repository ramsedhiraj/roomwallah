package com.roomwallah.verification.application.service;

import java.util.UUID;

public interface BrokerRiskCalculationService {
    int calculateRiskScore(UUID userId);
}
