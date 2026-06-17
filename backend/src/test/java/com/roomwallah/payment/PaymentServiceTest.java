package com.roomwallah.payment;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.payment.application.event.PaymentCapturedEvent;
import com.roomwallah.payment.application.outbox.PaymentOutboxService;
import com.roomwallah.payment.application.service.*;
import com.roomwallah.payment.domain.entity.*;
import com.roomwallah.payment.domain.port.PaymentRepositoryPort;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests: idempotency, fraud rejection, status transitions, optimistic lock guard,
 * and the Saga capture orchestration.
 */
public class PaymentServiceTest {

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;

    @Mock
    private EscrowService escrowService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private FraudRiskService fraudRiskService;

    @Mock
    private PaymentLockService paymentLockService;

    @Mock
    private PaymentOutboxService paymentOutboxService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditPort auditPort;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID bookingId;
    private UUID tenantId;
    private UUID ownerId;
    private UUID paymentId;
    private BigDecimal amount;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        amount = new BigDecimal("10000.00");

        // Default: lock service executes the task directly
        when(paymentLockService.executeWithLock(anyString(), anyLong(), any())).thenAnswer(inv -> {
            Supplier<?> task = inv.getArgument(2);
            return task.get();
        });
    }

    // ========================= INITIATE PAYMENT TESTS =========================

    @Test
    public void testInitiatePayment_Success_ReturnsPendingPayment() {
        when(fraudRiskService.checkFraudRisk(bookingId, tenantId, ownerId, amount)).thenReturn(30);
        Payment savedPayment = buildPayment(paymentId, PaymentStatus.PENDING);
        when(paymentRepositoryPort.save(any(Payment.class))).thenReturn(savedPayment);

        Payment result = paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, "INR", "STRIPE", "key-001");

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepositoryPort, times(1)).save(any(Payment.class));
    }

    @Test
    public void testInitiatePayment_Idempotency_ReturnsSavedPayment() {
        String idempotencyKey = "idem-key-999";
        Payment existing = buildPayment(paymentId, PaymentStatus.PENDING);
        when(paymentRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        Payment result = paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, "INR", "STRIPE", idempotencyKey);

        // Must return the existing payment, not create a new one
        assertEquals(existing.getId(), result.getId());
        verify(paymentRepositoryPort, never()).save(any());
        verify(fraudRiskService, never()).checkFraudRisk(any(), any(), any(), any());
    }

    @Test
    public void testInitiatePayment_HighFraudRisk_ThrowsSecurityException() {
        when(fraudRiskService.checkFraudRisk(bookingId, tenantId, ownerId, amount)).thenReturn(90);
        when(paymentRepositoryPort.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        SecurityException ex = assertThrows(SecurityException.class,
                () -> paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, "INR", "STRIPE", null));

        assertTrue(ex.getMessage().contains("fraud"));
        // Audit log and notification must be called for fraud blocks
        verify(auditPort, times(1)).log(eq("PAYMENT_FRAUD_BLOCKED"), anyString(), anyString(), any());
        verify(notificationPort, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void testInitiatePayment_BorderlineFraudScore84_IsApproved() {
        when(fraudRiskService.checkFraudRisk(bookingId, tenantId, ownerId, amount)).thenReturn(84);
        Payment savedPayment = buildPayment(paymentId, PaymentStatus.PENDING);
        when(paymentRepositoryPort.save(any(Payment.class))).thenReturn(savedPayment);

        // Score 84 is below the reject threshold (85), should succeed
        assertDoesNotThrow(() ->
                paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, "INR", "STRIPE", null));
    }

    @Test
    public void testInitiatePayment_BorderlineFraudScore85_IsRejected() {
        when(fraudRiskService.checkFraudRisk(bookingId, tenantId, ownerId, amount)).thenReturn(85);
        when(paymentRepositoryPort.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        assertThrows(SecurityException.class,
                () -> paymentService.initiatePayment(bookingId, tenantId, ownerId, amount, "INR", "STRIPE", null));
    }

    // ========================= CAPTURE PAYMENT TESTS =========================

    @Test
    public void testCapturePayment_Success_OrchestratesSaga() {
        Payment pendingPayment = buildPayment(paymentId, PaymentStatus.PENDING);
        pendingPayment.setBookingId(bookingId);
        pendingPayment.setTenantId(tenantId);
        pendingPayment.setOwnerId(ownerId);
        pendingPayment.setAmount(amount);
        pendingPayment.setCurrency("INR");

        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepositoryPort.save(any(Payment.class))).thenReturn(pendingPayment);

        EscrowAccount escrow = new EscrowAccount();
        escrow.setId(UUID.randomUUID());
        when(escrowService.holdFunds(any(), any(), any(), any(), any(), any())).thenReturn(escrow);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        when(invoiceService.generateInvoice(any(), any(), any(), any(), anyString(), any())).thenReturn(invoice);

        // WHEN
        Payment result = paymentService.capturePayment(paymentId, "ch_stripe_captured");

        assertNotNull(result);
        // Verify Saga steps were executed
        verify(escrowService, times(1)).holdFunds(any(), any(), any(), any(), any(), any());
        verify(ledgerService, times(1)).postTransaction(anyString(), any());
        verify(invoiceService, times(1)).generateInvoice(any(), any(), any(), any(), anyString(), any());
        verify(paymentOutboxService, times(1)).persistEvent(anyString(), anyString(), any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(PaymentCapturedEvent.class));
    }

    @Test
    public void testCapturePayment_AlreadyCaptured_IsIdempotent() {
        Payment capturedPayment = buildPayment(paymentId, PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        Payment result = paymentService.capturePayment(paymentId, "ch_already_captured");

        assertEquals(PaymentStatus.CAPTURED, result.getStatus());
        // No Saga steps should run
        verify(escrowService, never()).holdFunds(any(), any(), any(), any(), any(), any());
        verify(ledgerService, never()).postTransaction(anyString(), any());
    }

    @Test
    public void testCapturePayment_PaymentFailed_ThrowsIllegalStateException() {
        Payment failedPayment = buildPayment(paymentId, PaymentStatus.FAILED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(failedPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.capturePayment(paymentId, "ch_foo"));

        verify(escrowService, never()).holdFunds(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testCapturePayment_PaymentNotFound_ThrowsResourceNotFoundException() {
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.capturePayment(paymentId, "ch_bar"));
    }

    @Test
    public void testCapturePayment_ExecutedUnderDistributedLock() {
        Payment pendingPayment = buildPayment(paymentId, PaymentStatus.PENDING);
        pendingPayment.setBookingId(bookingId);
        pendingPayment.setTenantId(tenantId);
        pendingPayment.setOwnerId(ownerId);
        pendingPayment.setAmount(amount);
        pendingPayment.setCurrency("INR");

        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepositoryPort.save(any())).thenReturn(pendingPayment);
        when(escrowService.holdFunds(any(), any(), any(), any(), any(), any())).thenReturn(new EscrowAccount());
        when(invoiceService.generateInvoice(any(), any(), any(), any(), anyString(), any())).thenReturn(new Invoice());

        paymentService.capturePayment(paymentId, "ch_locked");

        // The lock must have been acquired for the capture operation
        verify(paymentLockService, times(1)).executeWithLock(
                eq("lock:payment:capture:" + paymentId), anyLong(), any());
    }

    // ========================= FAIL PAYMENT TESTS =========================

    @Test
    public void testFailPayment_Success_MarksAsFailed() {
        Payment pendingPayment = buildPayment(paymentId, PaymentStatus.PENDING);
        pendingPayment.setTenantId(tenantId);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepositoryPort.save(any(Payment.class))).thenReturn(pendingPayment);

        Payment result = paymentService.failPayment(paymentId, "Insufficient funds");

        verify(auditPort, times(1)).log(eq("PAYMENT_FAILED"), anyString(), anyString(), any());
    }

    @Test
    public void testFailPayment_AlreadyCaptured_ThrowsIllegalStateException() {
        Payment capturedPayment = buildPayment(paymentId, PaymentStatus.CAPTURED);
        when(paymentRepositoryPort.findById(paymentId)).thenReturn(Optional.of(capturedPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.failPayment(paymentId, "Cannot fail"));
    }

    // ========================= HELPER METHODS ========================

    private Payment buildPayment(UUID id, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setStatus(status);
        payment.setBookingId(bookingId);
        payment.setTenantId(tenantId);
        payment.setOwnerId(ownerId);
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setGatewayProvider("STRIPE");
        return payment;
    }
}
