package com.roomwallah.payment.domain.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayPort {
    String providerName();
    String createPaymentIntent(UUID paymentId, BigDecimal amount, String currency, String idempotencyKey);
    String capturePayment(String paymentIntentId);
    String issueRefund(String gatewayPaymentId, BigDecimal amount, String reason);
    String initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount);
    boolean verifyWebhookSignature(String payload, String signature, String secret);
}
