package com.roomwallah.verification.domain.port;

import java.util.UUID;

public interface FraudDetectionPort {
    int calculateRiskScore(UUID userId, String correlationId);
    void detectAndReport(UUID userId, String correlationId);
}
