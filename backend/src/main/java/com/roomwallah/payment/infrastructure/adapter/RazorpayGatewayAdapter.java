package com.roomwallah.payment.infrastructure.adapter;

import com.roomwallah.payment.domain.port.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Razorpay payment gateway adapter.
 *
 * <p>Production integration replaces stubs with Razorpay Java SDK calls:
 * {@code new RazorpayClient(keyId, keySecret).orders.create(orderRequest)} etc.
 * Webhook signature: Razorpay sends HMAC-SHA256 of the raw request body, keyed with the webhook secret.
 * Header name: {@code X-Razorpay-Signature}.
 */
@Slf4j
@Component
public class RazorpayGatewayAdapter implements PaymentGatewayPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public String providerName() {
        return "RAZORPAY";
    }

    @Override
    public String createPaymentIntent(UUID paymentId, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("[Razorpay] Creating order for paymentId={}, amount={} {}, idempotencyKey={}",
                paymentId, amount, currency, idempotencyKey);
        // Production: JSONObject orderRequest = new JSONObject(); razorpayClient.orders.create(orderRequest)
        return "rzp_order_mock_" + paymentId.toString().replace("-", "");
    }

    @Override
    public String capturePayment(String paymentIntentId) {
        log.info("[Razorpay] Capturing payment: {}", paymentIntentId);
        // Production: razorpayClient.payments.capture(paymentId, captureRequest)
        return "rzp_pay_captured_" + System.currentTimeMillis();
    }

    @Override
    public String issueRefund(String gatewayPaymentId, BigDecimal amount, String reason) {
        log.info("[Razorpay] Issuing refund for gatewayPaymentId={}, amount={}, reason={}",
                gatewayPaymentId, amount, reason);
        // Production: razorpayClient.payments.refund(paymentId, refundRequest)
        return "rzp_rfnd_mock_" + System.currentTimeMillis();
    }

    @Override
    public String initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount) {
        log.info("[Razorpay] Initiating payout for ownerId={}, amount={}, destination={}",
                ownerId, amount, destinationAccount);
        // Production: razorpayClient.payouts.create(payoutRequest) via Razorpay X API
        return "rzp_pout_mock_" + System.currentTimeMillis();
    }

    /**
     * Verifies a Razorpay webhook signature.
     *
     * <p>Razorpay computes HMAC-SHA256 of the raw JSON body using the webhook secret,
     * and sends the resulting hex string in the {@code X-Razorpay-Signature} header.
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            log.warn("[Razorpay] Missing payload, signature, or secret for webhook verification");
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(digest);

            boolean valid = computed.equalsIgnoreCase(signature.trim());
            if (!valid) {
                log.warn("[Razorpay] Webhook signature mismatch. Potential replay or tampering detected.");
            }
            return valid;
        } catch (Exception e) {
            log.error("[Razorpay] Webhook signature verification failed with exception", e);
            return false;
        }
    }
}
