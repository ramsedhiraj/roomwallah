package com.roomwallah.payment.application.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface FraudRiskService {
    int checkFraudRisk(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount);
}
