package com.roomwallah.payment.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.payment.application.event.PaymentCapturedEvent;
import com.roomwallah.payment.application.outbox.PaymentOutboxService;
import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.Invoice;
import com.roomwallah.payment.domain.entity.InvoiceType;
import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerEntryType;
import com.roomwallah.payment.domain.entity.Payment;
import com.roomwallah.payment.domain.entity.PaymentStatus;
import com.roomwallah.payment.domain.port.PaymentRepositoryPort;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final EscrowService escrowService;
    private final LedgerService ledgerService;
    private final InvoiceService invoiceService;
    private final FraudRiskService fraudRiskService;
    private final PaymentLockService paymentLockService;
    private final PaymentOutboxService paymentOutboxService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;

    @Override
    @Transactional
    public Payment initiatePayment(UUID bookingId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency, String gatewayProvider, String idempotencyKey) {
        log.info("Initiating payment for booking: {}, tenant: {}, amount: {}", bookingId, tenantId, amount);

        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payment> existing = paymentRepositoryPort.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Payment already exists for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // 2. Fraud Risk Assessment
        int riskScore = fraudRiskService.checkFraudRisk(bookingId, tenantId, ownerId, amount);
        String riskDecision = riskScore >= 85 ? "REJECT" : "APPROVE";

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setTenantId(tenantId);
        payment.setOwnerId(ownerId);
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "INR");
        payment.setGatewayProvider(gatewayProvider);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setRiskScore(riskScore);
        payment.setRiskDecision(riskDecision);

        if ("REJECT".equals(riskDecision)) {
            payment.setStatus(PaymentStatus.FAILED);
            Payment saved = paymentRepositoryPort.save(payment);
            
            // Audit and notification for fraud block
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("riskScore", riskScore);
            auditDetails.put("amount", amount);
            auditPort.log("PAYMENT_FRAUD_BLOCKED", tenantId.toString(), "SYSTEM", auditDetails);
            
            notificationPort.sendEmail("admin@roomwallah.com", "Payment Blocked - High Fraud Risk", 
                    "Payment ID: " + saved.getId() + " was blocked. Risk score: " + riskScore);
            
            throw new SecurityException("Payment rejected due to high fraud risk score: " + riskScore);
        }

        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepositoryPort.save(payment);
        
        log.info("Payment initiated successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Payment capturePayment(UUID paymentId, String gatewayPaymentId) {
        String lockKey = "lock:payment:capture:" + paymentId;
        
        // Execute capture within Redis-based distributed lock to prevent concurrency issues
        return paymentLockService.executeWithLock(lockKey, 30, () -> {
            log.info("Capturing payment ID: {} with gateway payment ID: {}", paymentId, gatewayPaymentId);
            
            Payment payment = paymentRepositoryPort.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                log.warn("Payment ID: {} is already CAPTURED. Returning existing.", paymentId);
                return payment;
            }

            if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.REFUNDED) {
                throw new IllegalStateException("Payment cannot be captured from status: " + payment.getStatus());
            }

            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setGatewayPaymentId(gatewayPaymentId);
            Payment savedPayment = paymentRepositoryPort.save(payment);

            // 1. Saga: Update Escrow (Hold funds)
            EscrowAccount escrowAccount = escrowService.holdFunds(
                    savedPayment.getBookingId(),
                    savedPayment.getId(),
                    savedPayment.getTenantId(),
                    savedPayment.getOwnerId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency()
            );

            // 2. Saga: Post ledger entries (Debit Cash, Credit Escrow Liability)
            LedgerEntry debitCash = new LedgerEntry();
            debitCash.setAccountNumber("CASH");
            debitCash.setEntryType(LedgerEntryType.DEBIT);
            debitCash.setAmount(savedPayment.getAmount());
            debitCash.setCurrency(savedPayment.getCurrency());

            LedgerEntry creditEscrow = new LedgerEntry();
            creditEscrow.setAccountNumber("ESCROW_LIABILITY");
            creditEscrow.setEntryType(LedgerEntryType.CREDIT);
            creditEscrow.setAmount(savedPayment.getAmount());
            creditEscrow.setCurrency(savedPayment.getCurrency());

            ledgerService.postTransaction(
                    "Capture payment and hold funds in escrow for booking ID: " + savedPayment.getBookingId(),
                    List.of(debitCash, creditEscrow)
            );

            // 3. Saga: Generate Invoice
            BillingAddress billingAddress = BillingAddress.builder()
                    .street("123 Booking St")
                    .city("Bangalore")
                    .state("Karnataka")
                    .country("India")
                    .zipCode("560001")
                    .build();

            Invoice invoice = invoiceService.generateInvoice(
                    savedPayment.getBookingId(),
                    savedPayment.getId(),
                    InvoiceType.RECEIPT,
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    billingAddress
            );

            // 4. Saga: Publish Outbox Event (shared transactional outbox)
            PaymentCapturedEvent domainEvent = PaymentCapturedEvent.builder()
                    .paymentId(savedPayment.getId())
                    .bookingId(savedPayment.getBookingId())
                    .tenantId(savedPayment.getTenantId())
                    .ownerId(savedPayment.getOwnerId())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .gatewayPaymentId(gatewayPaymentId)
                    .capturedAt(Instant.now())
                    .build();

            paymentOutboxService.persistEvent("PAYMENT", savedPayment.getId().toString(), domainEvent);

            // 5. Saga: Publish Spring Event (local notification/listener)
            applicationEventPublisher.publishEvent(domainEvent);

            log.info("Payment ID: {} captured successfully", savedPayment.getId());
            return savedPayment;
        });
    }

    @Override
    @Transactional
    public Payment failPayment(UUID paymentId, String errorReason) {
        log.info("Failing payment ID: {}, reason: {}", paymentId, errorReason);
        Payment payment = paymentRepositoryPort.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Cannot fail a captured payment");
        }

        payment.setStatus(PaymentStatus.FAILED);
        Payment saved = paymentRepositoryPort.save(payment);

        // Audit log
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("errorReason", errorReason);
        auditPort.log("PAYMENT_FAILED", payment.getTenantId().toString(), "SYSTEM", auditDetails);

        log.info("Payment ID: {} marked as FAILED", paymentId);
        return saved;
    }
}
