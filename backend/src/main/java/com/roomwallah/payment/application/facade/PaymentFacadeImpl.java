package com.roomwallah.payment.application.facade;

import com.roomwallah.payment.application.dto.*;
import com.roomwallah.payment.application.service.*;
import com.roomwallah.payment.domain.entity.*;
import com.roomwallah.payment.domain.port.*;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentService paymentService;
    private final EscrowService escrowService;
    private final RefundService refundService;
    private final PayoutService payoutService;
    private final InvoiceService invoiceService;
    private final WebhookService webhookService;
    private final ReconciliationService reconciliationService;

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final EscrowRepositoryPort escrowRepositoryPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final DisputeRepositoryPort disputeRepositoryPort;
    private final WebhookRepositoryPort webhookRepositoryPort;

    // ── Payment operations ──────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency, String gatewayProvider, String idempotencyKey) {
        log.info("Facade: Initiating payment for booking {}", bookingId);
        Payment payment = paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, currency, gatewayProvider, idempotencyKey);
        return mapToDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto capturePayment(UUID paymentId, String gatewayPaymentId) {
        log.info("Facade: Capturing payment ID {}", paymentId);
        Payment payment = paymentService.capturePayment(paymentId, gatewayPaymentId);
        return mapToDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto failPayment(UUID paymentId, String errorReason) {
        log.info("Facade: Failing payment ID {}", paymentId);
        Payment payment = paymentService.failPayment(paymentId, errorReason);
        return mapToDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(UUID paymentId) {
        log.info("Facade: Getting payment ID {}", paymentId);
        Payment payment = paymentRepositoryPort.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return mapToDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getMyPayments(UUID tenantId) {
        log.info("Facade: Getting payments for tenant {}", tenantId);
        return paymentRepositoryPort.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        log.info("Facade: Getting all payments");
        return paymentRepositoryPort.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── Escrow operations ───────────────────────────────────────────────

    @Override
    @Transactional
    public EscrowAccountResponseDto holdFunds(UUID bookingId, UUID paymentId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency) {
        log.info("Facade: Holding funds for booking {}", bookingId);
        EscrowAccount account = escrowService.holdFunds(bookingId, paymentId, tenantId, ownerId, amount, currency);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public EscrowAccountResponseDto releaseFunds(UUID escrowAccountId) {
        log.info("Facade: Releasing funds from escrow account {}", escrowAccountId);
        EscrowAccount account = escrowService.releaseFunds(escrowAccountId);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public EscrowAccountResponseDto refundEscrow(UUID escrowAccountId) {
        log.info("Facade: Refunding escrow account {}", escrowAccountId);
        EscrowAccount account = escrowService.refundEscrow(escrowAccountId);
        return mapToDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscrowAccountResponseDto> getOwnerEscrowAccounts(UUID ownerId) {
        log.info("Facade: Getting escrow accounts for owner {}", ownerId);
        return escrowRepositoryPort.findByOwnerId(ownerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── Refund operations ───────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponseDto initiateRefund(UUID paymentId, BigDecimal amount, String reason) {
        log.info("Facade: Initiating refund for payment {}", paymentId);
        Refund refund = refundService.initiateRefund(paymentId, amount, reason);
        return mapToDto(refund);
    }

    @Override
    @Transactional
    public RefundResponseDto processRefundSuccess(UUID refundId, String gatewayRefundId) {
        log.info("Facade: Processing successful refund ID {}", refundId);
        Refund refund = refundService.processRefundSuccess(refundId, gatewayRefundId);
        return mapToDto(refund);
    }

    // ── Payout operations ───────────────────────────────────────────────

    @Override
    @Transactional
    public PayoutResponseDto initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount) {
        log.info("Facade: Initiating payout for owner {}", ownerId);
        Payout payout = payoutService.initiatePayout(ownerId, amount, destinationAccount);
        return mapToDto(payout);
    }

    @Override
    @Transactional
    public PayoutResponseDto settlePayout(UUID payoutId, String gatewayPayoutId) {
        log.info("Facade: Settling payout ID {}", payoutId);
        Payout payout = payoutService.settlePayout(payoutId, gatewayPayoutId);
        return mapToDto(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponseDto> getOwnerPayouts(UUID ownerId) {
        log.info("Facade: Getting payouts for owner {}", ownerId);
        return payoutRepositoryPort.findByOwnerId(ownerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── Invoice operations ──────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponseDto generateInvoice(UUID bookingId, UUID paymentId, InvoiceType type, BigDecimal amount, String currency, BillingAddress billingAddress) {
        log.info("Facade: Generating invoice for booking {}", bookingId);
        Invoice invoice = invoiceService.generateInvoice(bookingId, paymentId, type, amount, currency, billingAddress);
        return mapToDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoice(UUID invoiceId) {
        log.info("Facade: Getting invoice ID {}", invoiceId);
        Invoice invoice = invoiceRepositoryPort.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        return mapToDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceByBookingId(UUID bookingId) {
        log.info("Facade: Getting invoice by booking ID {}", bookingId);
        Invoice invoice = invoiceRepositoryPort.findByBookingId(bookingId).orElse(null);
        return mapToDto(invoice);
    }

    // ── Webhook operations ──────────────────────────────────────────────

    @Override
    @Transactional
    public void processWebhook(String gatewayProvider, String eventType, String payloadJson) {
        log.info("Facade: Processing webhook event {} from {}", eventType, gatewayProvider);
        webhookService.processWebhook(gatewayProvider, eventType, payloadJson);
    }

    @Override
    @Transactional
    public void retryWebhook(UUID webhookId) {
        log.info("Facade: Retrying webhook ID {}", webhookId);
        PaymentWebhook webhook = webhookRepositoryPort.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + webhookId));
        webhookService.processWebhook(webhook.getGatewayProvider(), webhook.getEventType(), webhook.getPayloadJson());
    }

    // ── Admin operations ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void reconcileGatewayTransactions(String gatewayProvider, List<ReconciliationService.GatewayRecord> records) {
        log.info("Facade: Reconciling gateway transactions for {}", gatewayProvider);
        reconciliationService.reconcileGatewayTransactions(gatewayProvider, records);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponseDto> getAllDisputes() {
        log.info("Facade: Getting all disputes");
        return disputeRepositoryPort.findAll().stream()
                .map(this::mapToDisputeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookResponseDto> getAllWebhooks() {
        log.info("Facade: Getting all webhooks");
        return webhookRepositoryPort.findAll().stream()
                .map(this::mapToWebhookDto)
                .collect(Collectors.toList());
    }

    // ── Mappers ─────────────────────────────────────────────────────────

    private PaymentResponseDto mapToDto(Payment payment) {
        if (payment == null) return null;
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .tenantId(payment.getTenantId())
                .ownerId(payment.getOwnerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .gatewayProvider(payment.getGatewayProvider())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .idempotencyKey(payment.getIdempotencyKey())
                .riskScore(payment.getRiskScore())
                .riskDecision(payment.getRiskDecision())
                .build();
    }

    private EscrowAccountResponseDto mapToDto(EscrowAccount account) {
        if (account == null) return null;
        return EscrowAccountResponseDto.builder()
                .id(account.getId())
                .bookingId(account.getBookingId())
                .paymentId(account.getPaymentId())
                .tenantId(account.getTenantId())
                .ownerId(account.getOwnerId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .heldAt(account.getHeldAt())
                .releasedAt(account.getReleasedAt())
                .build();
    }

    private RefundResponseDto mapToDto(Refund refund) {
        if (refund == null) return null;
        return RefundResponseDto.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus() != null ? refund.getStatus().name() : null)
                .gatewayRefundId(refund.getGatewayRefundId())
                .reason(refund.getReason())
                .build();
    }

    private PayoutResponseDto mapToDto(Payout payout) {
        if (payout == null) return null;
        return PayoutResponseDto.builder()
                .id(payout.getId())
                .ownerId(payout.getOwnerId())
                .amount(payout.getAmount())
                .currency(payout.getCurrency())
                .status(payout.getStatus() != null ? payout.getStatus().name() : null)
                .gatewayPayoutId(payout.getGatewayPayoutId())
                .destinationAccount(payout.getDestinationAccount())
                .build();
    }

    private InvoiceResponseDto mapToDto(Invoice invoice) {
        if (invoice == null) return null;
        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .bookingId(invoice.getBookingId())
                .paymentId(invoice.getPaymentId())
                .refundId(invoice.getRefundId())
                .type(invoice.getType() != null ? invoice.getType().name() : null)
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .pdfPath(invoice.getPdfPath())
                .build();
    }

    private DisputeResponseDto mapToDisputeDto(PaymentDispute dispute) {
        if (dispute == null) return null;
        return DisputeResponseDto.builder()
                .id(dispute.getId())
                .paymentId(dispute.getPaymentId())
                .reason(dispute.getReason())
                .amount(dispute.getAmount())
                .currency(dispute.getCurrency())
                .status(dispute.getStatus() != null ? dispute.getStatus().name() : null)
                .evidenceJson(dispute.getEvidenceJson())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }

    private WebhookResponseDto mapToWebhookDto(PaymentWebhook webhook) {
        if (webhook == null) return null;
        return WebhookResponseDto.builder()
                .id(webhook.getId())
                .gatewayProvider(webhook.getGatewayProvider())
                .eventType(webhook.getEventType())
                .payloadJson(webhook.getPayloadJson())
                .processed(webhook.isProcessed())
                .processedAt(webhook.getProcessedAt())
                .errorReason(webhook.getErrorReason())
                .createdAt(webhook.getCreatedAt())
                .updatedAt(webhook.getUpdatedAt())
                .build();
    }
}
