package com.roomwallah.payment;

import com.roomwallah.payment.application.service.*;
import com.roomwallah.payment.domain.entity.*;
import com.roomwallah.payment.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefundServiceImpl.
 * Covers: refund initiation, idempotent success processing, invalid state transitions,
 * amount boundary checks, and the Saga for escrow refund + invoice generation.
 */
public class RefundServiceTest {

    @Mock
    private RefundRepositoryPort refundRepositoryPort;

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;

    @Mock
    private EscrowRepositoryPort escrowRepositoryPort;

    @Mock
    private EscrowService escrowService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceRepositoryPort invoiceRepositoryPort;

    @Mock
    private PaymentLockService paymentLockService;

    @InjectMocks
    private RefundServiceImpl refundService;

    private UUID paymentId;
    private UUID tenantId;
    private UUID ownerId;
    private UUID bookingId;
    private UUID refundId;
    private BigDecimal paymentAmount;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        refundId = UUID.randomUUID();
        paymentAmount = new BigDecimal("10000.00");

        // Default: lock service runs task directly
        when(paymentLockService.executeWithLock(anyString(), anyLong(), any())).thenAnswer(inv -> {
            Supplier<?> task = inv.getArgument(2);
            return task.get();
        });
    }

    // ========================= INITIATE REFUND TESTS =========================

    @Test
    public void testInitiateRefund_FullRefund_Success() {
        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        Refund savedRefund = buildRefund(RefundStatus.PENDING, paymentAmount);
        when(refundRepositoryPort.save(any(Refund.class))).thenReturn(savedRefund);

        Refund result = refundService.initiateRefund(paymentId, paymentAmount, "Tenant requested full refund");

        assertNotNull(result);
        assertEquals(RefundStatus.PENDING, result.getStatus());
        verify(refundRepositoryPort, times(1)).save(any(Refund.class));
    }

    @Test
    public void testInitiateRefund_PartialRefund_Success() {
        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        BigDecimal partialAmount = new BigDecimal("5000.00");
        Refund savedRefund = buildRefund(RefundStatus.PENDING, partialAmount);
        when(refundRepositoryPort.save(any(Refund.class))).thenReturn(savedRefund);

        Refund result = refundService.initiateRefund(paymentId, partialAmount, "Partial refund for amenity issues");

        assertNotNull(result);
        assertEquals(RefundStatus.PENDING, result.getStatus());
    }

    @Test
    public void testInitiateRefund_PaymentNotCaptured_ThrowsIllegalStateException() {
        Payment pendingPayment = buildPayment(PaymentStatus.PENDING);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(pendingPayment));

        assertThrows(IllegalStateException.class,
                () -> refundService.initiateRefund(paymentId, paymentAmount, "Invalid state refund"));
    }

    @Test
    public void testInitiateRefund_AmountExceedsPayment_ThrowsIllegalArgumentException() {
        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        BigDecimal excessAmount = paymentAmount.add(new BigDecimal("1.00"));

        assertThrows(IllegalArgumentException.class,
                () -> refundService.initiateRefund(paymentId, excessAmount, "Excess refund attempt"));
    }

    @Test
    public void testInitiateRefund_ZeroAmount_ThrowsIllegalArgumentException() {
        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        assertThrows(IllegalArgumentException.class,
                () -> refundService.initiateRefund(paymentId, BigDecimal.ZERO, "Zero amount refund"));
    }

    @Test
    public void testInitiateRefund_NegativeAmount_ThrowsIllegalArgumentException() {
        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        assertThrows(IllegalArgumentException.class,
                () -> refundService.initiateRefund(paymentId, new BigDecimal("-500"), "Negative refund"));
    }

    // ========================= PROCESS REFUND SUCCESS TESTS =========================

    @Test
    public void testProcessRefundSuccess_Success_OrchestratesSaga() {
        Refund pendingRefund = buildRefund(RefundStatus.PENDING, paymentAmount);
        pendingRefund.setPaymentId(paymentId);
        when(refundRepositoryPort.findById(refundId)).thenReturn(Optional.of(pendingRefund));

        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        capturedPayment.setBookingId(bookingId);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(UUID.randomUUID());
        when(escrowRepositoryPort.findByBookingId(bookingId)).thenReturn(Optional.of(escrow));
        when(escrowService.refundEscrow(any())).thenReturn(escrow);

        Refund successRefund = buildRefund(RefundStatus.SUCCEEDED, paymentAmount);
        when(refundRepositoryPort.save(any(Refund.class))).thenReturn(successRefund);
        when(paymentRepositoryPort.save(any(Payment.class))).thenReturn(capturedPayment);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        when(invoiceService.generateInvoice(any(), any(), any(), any(), anyString(), any())).thenReturn(invoice);
        when(invoiceRepositoryPort.save(any(Invoice.class))).thenReturn(invoice);

        Refund result = refundService.processRefundSuccess(refundId, "re_gateway_123");

        assertNotNull(result);
        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());
        // Verify Saga executed
        verify(escrowService, times(1)).refundEscrow(any());
        verify(invoiceService, times(1)).generateInvoice(any(), any(), eq(InvoiceType.REFUND_RECEIPT), any(), anyString(), any());
        verify(invoiceRepositoryPort, times(1)).save(any(Invoice.class));
    }

    @Test
    public void testProcessRefundSuccess_AlreadySucceeded_IsIdempotent() {
        Refund succeededRefund = buildRefund(RefundStatus.SUCCEEDED, paymentAmount);
        when(refundRepositoryPort.findById(refundId)).thenReturn(Optional.of(succeededRefund));

        Refund result = refundService.processRefundSuccess(refundId, "re_already_done");

        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());
        // Should not run Saga steps again
        verify(escrowService, never()).refundEscrow(any());
        verify(invoiceService, never()).generateInvoice(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    public void testProcessRefundSuccess_ExecutedUnderDistributedLock() {
        Refund pendingRefund = buildRefund(RefundStatus.PENDING, paymentAmount);
        pendingRefund.setPaymentId(paymentId);
        when(refundRepositoryPort.findById(refundId)).thenReturn(Optional.of(pendingRefund));

        Payment capturedPayment = buildPayment(PaymentStatus.CAPTURED);
        capturedPayment.setBookingId(bookingId);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));
        when(paymentRepositoryPort.save(any())).thenReturn(capturedPayment);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(UUID.randomUUID());
        when(escrowRepositoryPort.findByBookingId(bookingId)).thenReturn(Optional.of(escrow));
        when(escrowService.refundEscrow(any())).thenReturn(escrow);

        Refund successRefund = buildRefund(RefundStatus.SUCCEEDED, paymentAmount);
        when(refundRepositoryPort.save(any())).thenReturn(successRefund);
        when(invoiceService.generateInvoice(any(), any(), any(), any(), anyString(), any())).thenReturn(new Invoice());
        when(invoiceRepositoryPort.save(any())).thenReturn(new Invoice());

        refundService.processRefundSuccess(refundId, "re_lock_test");

        verify(paymentLockService, times(1)).executeWithLock(
                eq("lock:refund:process:" + refundId), anyLong(), any());
    }

    // ========================= HELPER METHODS ========================

    private Payment buildPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(status);
        payment.setAmount(paymentAmount);
        payment.setCurrency("INR");
        payment.setTenantId(tenantId);
        payment.setOwnerId(ownerId);
        payment.setBookingId(bookingId);
        return payment;
    }

    private Refund buildRefund(RefundStatus status, BigDecimal amount) {
        Refund refund = new Refund();
        refund.setId(refundId);
        refund.setPaymentId(paymentId);
        refund.setAmount(amount);
        refund.setCurrency("INR");
        refund.setStatus(status);
        refund.setReason("Test reason");
        return refund;
    }
}
