package com.roomwallah.payment.application.facade;

import com.roomwallah.payment.application.dto.*;
import com.roomwallah.payment.application.service.ReconciliationService;
import com.roomwallah.payment.domain.entity.InvoiceType;
import com.roomwallah.payment.domain.valueobject.BillingAddress;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentFacade {

    PaymentResponseDto initiatePayment(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency, String gatewayProvider, String idempotencyKey);
    PaymentResponseDto capturePayment(UUID paymentId, String gatewayPaymentId);
    PaymentResponseDto failPayment(UUID paymentId, String errorReason);
    PaymentResponseDto getPayment(UUID paymentId);
    List<PaymentResponseDto> getMyPayments(UUID tenantId);
    List<PaymentResponseDto> getAllPayments();

    EscrowAccountResponseDto holdFunds(UUID bookingId, UUID paymentId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency);
    EscrowAccountResponseDto releaseFunds(UUID escrowAccountId);
    EscrowAccountResponseDto refundEscrow(UUID escrowAccountId);
    List<EscrowAccountResponseDto> getOwnerEscrowAccounts(UUID ownerId);

    RefundResponseDto initiateRefund(UUID paymentId, BigDecimal amount, String reason);
    RefundResponseDto processRefundSuccess(UUID refundId, String gatewayRefundId);

    PayoutResponseDto initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount);
    PayoutResponseDto settlePayout(UUID payoutId, String gatewayPayoutId);
    List<PayoutResponseDto> getOwnerPayouts(UUID ownerId);

    InvoiceResponseDto generateInvoice(UUID bookingId, UUID paymentId, InvoiceType type, BigDecimal amount, String currency, BillingAddress billingAddress);
    InvoiceResponseDto getInvoice(UUID invoiceId);
    InvoiceResponseDto getInvoiceByBookingId(UUID bookingId);

    void processWebhook(String gatewayProvider, String eventType, String payloadJson);
    void retryWebhook(UUID webhookId);

    void reconcileGatewayTransactions(String gatewayProvider, List<ReconciliationService.GatewayRecord> records);

    List<DisputeResponseDto> getAllDisputes();
    List<WebhookResponseDto> getAllWebhooks();
}

