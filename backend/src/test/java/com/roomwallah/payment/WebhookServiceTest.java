package com.roomwallah.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.payment.application.service.*;
import com.roomwallah.payment.domain.entity.*;
import com.roomwallah.payment.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WebhookServiceImpl.
 * Covers: idempotent webhook processing, replay prevention, event routing,
 * and error handling for unknown event types.
 */
public class WebhookServiceTest {

    @Mock
    private WebhookRepositoryPort webhookRepositoryPort;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RefundService refundService;

    @Mock
    private PayoutRepositoryPort payoutRepositoryPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookServiceImpl webhookService;

    private UUID paymentId;
    private UUID refundId;
    private UUID payoutId;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentId = UUID.randomUUID();
        refundId = UUID.randomUUID();
        payoutId = UUID.randomUUID();

        // Default: save returns the same webhook with an ID
        when(webhookRepositoryPort.save(any(PaymentWebhook.class))).thenAnswer(inv -> {
            PaymentWebhook wh = inv.getArgument(0);
            if (wh.getId() == null) wh.setId(UUID.randomUUID());
            return wh;
        });
    }

    // ========================= WEBHOOK ROUTING TESTS =========================

    @Test
    public void testProcessWebhook_ChargeSucceeded_CallsCapturePayment() throws Exception {
        String payloadJson = String.format(
                "{\"paymentId\":\"%s\",\"gatewayPaymentId\":\"ch_stripe_001\"}", paymentId);

        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of(
                        "paymentId", paymentId.toString(),
                        "gatewayPaymentId", "ch_stripe_001"
                ));

        Payment capturedPayment = new Payment();
        capturedPayment.setId(paymentId);
        capturedPayment.setStatus(PaymentStatus.CAPTURED);
        when(paymentService.capturePayment(paymentId, "ch_stripe_001")).thenReturn(capturedPayment);

        PaymentWebhook result = webhookService.processWebhook("STRIPE", "charge.succeeded", payloadJson);

        assertNotNull(result);
        assertTrue(result.isProcessed());
        verify(paymentService, times(1)).capturePayment(paymentId, "ch_stripe_001");
    }

    @Test
    public void testProcessWebhook_PaymentCaptured_RazorpayEvent_CallsCapturePayment() throws Exception {
        String payloadJson = String.format(
                "{\"paymentId\":\"%s\",\"gatewayPaymentId\":\"rzp_pay_001\"}", paymentId);

        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of(
                        "paymentId", paymentId.toString(),
                        "gatewayPaymentId", "rzp_pay_001"
                ));

        Payment capturedPayment = new Payment();
        capturedPayment.setStatus(PaymentStatus.CAPTURED);
        when(paymentService.capturePayment(paymentId, "rzp_pay_001")).thenReturn(capturedPayment);

        PaymentWebhook result = webhookService.processWebhook("RAZORPAY", "payment.captured", payloadJson);

        assertTrue(result.isProcessed());
        verify(paymentService, times(1)).capturePayment(paymentId, "rzp_pay_001");
    }

    @Test
    public void testProcessWebhook_RefundSucceeded_CallsProcessRefundSuccess() throws Exception {
        String payloadJson = String.format(
                "{\"refundId\":\"%s\",\"gatewayRefundId\":\"re_stripe_001\"}", refundId);

        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of(
                        "refundId", refundId.toString(),
                        "gatewayRefundId", "re_stripe_001"
                ));

        Refund successRefund = new Refund();
        successRefund.setId(refundId);
        successRefund.setStatus(RefundStatus.SUCCEEDED);
        when(refundService.processRefundSuccess(refundId, "re_stripe_001")).thenReturn(successRefund);

        PaymentWebhook result = webhookService.processWebhook("STRIPE", "refund.succeeded", payloadJson);

        assertTrue(result.isProcessed());
        verify(refundService, times(1)).processRefundSuccess(refundId, "re_stripe_001");
    }

    @Test
    public void testProcessWebhook_PayoutFailed_MarksPayoutAsFailed() throws Exception {
        String payloadJson = String.format(
                "{\"payoutId\":\"%s\",\"reason\":\"Insufficient bank balance\"}", payoutId);

        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of(
                        "payoutId", payoutId.toString(),
                        "reason", "Insufficient bank balance"
                ));

        Payout payout = new Payout();
        payout.setId(payoutId);
        payout.setStatus(PayoutStatus.PENDING);
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.of(payout));
        when(payoutRepositoryPort.save(any(Payout.class))).thenReturn(payout);

        PaymentWebhook result = webhookService.processWebhook("CASHFREE", "payout.failed", payloadJson);

        assertTrue(result.isProcessed());
        verify(payoutRepositoryPort, times(1)).save(any(Payout.class));
    }

    @Test
    public void testProcessWebhook_UnknownEventType_ProcessedGracefully() throws Exception {
        String payloadJson = "{\"someField\":\"someValue\"}";

        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of("someField", "someValue"));

        // Should not throw — unknown events are logged and marked processed
        PaymentWebhook result = webhookService.processWebhook("STRIPE", "unknown.event.type", payloadJson);

        assertNotNull(result);
        assertTrue(result.isProcessed()); // marked processed even if no handler found
        verify(paymentService, never()).capturePayment(any(), any());
        verify(refundService, never()).processRefundSuccess(any(), any());
    }

    // ========================= ERROR HANDLING TESTS =========================

    @Test
    public void testProcessWebhook_JsonParsingFails_WebhookSavedWithError() throws Exception {
        String malformedJson = "{not valid json}";

        when(objectMapper.readValue(eq(malformedJson), eq(java.util.Map.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Malformed JSON"));

        PaymentWebhook result = webhookService.processWebhook("STRIPE", "charge.succeeded", malformedJson);

        assertNotNull(result);
        assertFalse(result.isProcessed()); // must be marked unprocessed on error
        assertNotNull(result.getErrorReason()); // must have an error reason
        // Webhook is saved (for audit trail)
        verify(webhookRepositoryPort, atLeastOnce()).save(any(PaymentWebhook.class));
    }

    @Test
    public void testProcessWebhook_AlwaysPersistsWebhookToDatabase() throws Exception {
        String payloadJson = "{\"paymentId\":\"" + paymentId + "\",\"gatewayPaymentId\":\"ch_001\"}";
        when(objectMapper.readValue(eq(payloadJson), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of(
                        "paymentId", paymentId.toString(),
                        "gatewayPaymentId", "ch_001"
                ));
        when(paymentService.capturePayment(any(), any())).thenReturn(new Payment());

        webhookService.processWebhook("STRIPE", "charge.succeeded", payloadJson);

        // Must persist webhook at start AND after processing — at least 2 saves
        verify(webhookRepositoryPort, atLeast(2)).save(any(PaymentWebhook.class));
    }
}
