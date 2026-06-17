package com.roomwallah.payment.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class FraudRiskServiceImpl implements FraudRiskService {

    @Override
    public int checkFraudRisk(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount) {
        log.info("Checking fraud risk for tenant: {}, owner: {}, amount: {}", tenantId, ownerId, amount);

        if (bookingId == null || tenantId == null || ownerId == null || amount == null) {
            log.warn("High fraud risk: null parameters detected");
            return 100;
        }

        if (tenantId.equals(ownerId)) {
            log.warn("High fraud risk: tenant ID is identical to owner ID");
            return 95;
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            log.warn("High fraud risk: amount exceeds transaction threshold limit");
            return 85;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("High fraud risk: negative or zero amount");
            return 90;
        }

        log.info("Fraud risk check passed with low score");
        return 10; // Low risk
    }
}
