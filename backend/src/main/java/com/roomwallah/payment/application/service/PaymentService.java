package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Payment;
import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    Payment initiatePayment(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency, String gatewayProvider, String idempotencyKey);
    Payment capturePayment(UUID paymentId, String gatewayPaymentId);
    Payment failPayment(UUID paymentId, String errorReason);
}
