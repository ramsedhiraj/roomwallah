package com.roomwallah.payment.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.Invoice;
import com.roomwallah.payment.domain.entity.InvoiceType;
import com.roomwallah.payment.domain.entity.Payment;
import com.roomwallah.payment.domain.entity.PaymentStatus;
import com.roomwallah.payment.domain.entity.Refund;
import com.roomwallah.payment.domain.entity.RefundStatus;
import com.roomwallah.payment.domain.port.EscrowRepositoryPort;
import com.roomwallah.payment.domain.port.InvoiceRepositoryPort;
import com.roomwallah.payment.domain.port.PaymentRepositoryPort;
import com.roomwallah.payment.domain.port.RefundRepositoryPort;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final EscrowRepositoryPort escrowRepositoryPort;
    private final EscrowService escrowService;
    private final InvoiceService invoiceService;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final PaymentLockService paymentLockService;

    @Override
    @Transactional
    public Refund initiateRefund(UUID paymentId, BigDecimal amount, String reason) {
        log.info("Initiating refund for payment ID: {}, amount: {}, reason: {}", paymentId, amount, reason);

        Payment payment = paymentRepositoryPort.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Refunds can only be initiated for CAPTURED payments. Current status: " + payment.getStatus());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }

        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(amount);
        refund.setCurrency(payment.getCurrency());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);

        Refund saved = refundRepositoryPort.save(refund);
        log.info("Refund initiated successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Refund processRefundSuccess(UUID refundId, String gatewayRefundId) {
        String lockKey = "lock:refund:process:" + refundId;

        return paymentLockService.executeWithLock(lockKey, 30, () -> {
            log.info("Processing successful refund ID: {}, gateway refund ID: {}", refundId, gatewayRefundId);

            Refund refund = refundRepositoryPort.findById(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Refund not found with ID: " + refundId));

            if (refund.getStatus() == RefundStatus.SUCCEEDED) {
                log.warn("Refund ID: {} is already SUCCEEDED.", refundId);
                return refund;
            }

            Payment payment = paymentRepositoryPort.findById(refund.getPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + refund.getPaymentId()));

            // Update Refund status
            refund.setStatus(RefundStatus.SUCCEEDED);
            refund.setGatewayRefundId(gatewayRefundId);
            Refund savedRefund = refundRepositoryPort.save(refund);

            // Update Payment status to REFUNDED (if fully refunded)
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepositoryPort.save(payment);

            // Update Escrow state & post Ledger entries (Debit Escrow Liability, Credit Cash)
            EscrowAccount escrow = escrowRepositoryPort.findByBookingId(payment.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escrow account not found for booking ID: " + payment.getBookingId()));

            escrowService.refundEscrow(escrow.getId());

            // Generate refund receipt invoice
            BillingAddress billingAddress = BillingAddress.builder()
                    .street("123 Booking St")
                    .city("Bangalore")
                    .state("Karnataka")
                    .country("India")
                    .zipCode("560001")
                    .build();

            Invoice invoice = invoiceService.generateInvoice(
                    payment.getBookingId(),
                    payment.getId(),
                    InvoiceType.REFUND_RECEIPT,
                    savedRefund.getAmount(),
                    savedRefund.getCurrency(),
                    billingAddress
            );

            // Associate refund ID with invoice and save
            invoice.setRefundId(savedRefund.getId());
            invoiceRepositoryPort.save(invoice);

            log.info("Refund ID: {} processed successfully", refundId);
            return savedRefund;
        });
    }
}
