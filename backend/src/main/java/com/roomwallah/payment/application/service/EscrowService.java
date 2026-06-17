package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.EscrowAccount;
import java.math.BigDecimal;
import java.util.UUID;

public interface EscrowService {
    EscrowAccount holdFunds(UUID bookingId, UUID paymentId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency);
    EscrowAccount releaseFunds(UUID escrowAccountId);
    EscrowAccount refundEscrow(UUID escrowAccountId);
}
