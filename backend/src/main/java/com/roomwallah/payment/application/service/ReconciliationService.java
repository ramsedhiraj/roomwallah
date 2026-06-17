package com.roomwallah.payment.application.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ReconciliationService {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class GatewayRecord {
        private String gatewayPaymentId;
        private BigDecimal amount;
        private String status;
        private Instant timestamp;
    }

    void reconcileGatewayTransactions(String gatewayProvider, List<GatewayRecord> gatewayRecords);
}
