package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Payout;
import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutService {
    Payout initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount);
    Payout settlePayout(UUID payoutId, String gatewayPayoutId);
}
