package com.roomwallah.payment.infrastructure.adapter;

import com.roomwallah.payment.domain.port.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Cashfree payment gateway adapter.
 *
 * <p>Production integration replaces stubs with Cashfree Java SDK calls.
 * Webhook signature: Cashfree computes HMAC-SHA256 of {@code <timestamp><payload>}
 * using the client secret and encodes it in Base64.
 * The timestamp is delivered in the {@code X-Webhook-Timestamp} header and
 * the signature in the {@code X-Webhook-Signature} header.
 *
 * <p>The {@code signature} parameter received here is expected to be the pre-joined
 * string {@code <timestamp>:<base64-signature>} so that this adapter can split and verify.
 * The {@link com.roomwallah.payment.presentation.controller.PaymentWebhookController}
 * joins them before calling this method.
 */
@Slf4j
@Component
public class CashfreeGatewayAdapter implements PaymentGatewayPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public String providerName() {
        return "CASHFREE";
    }

    @Override
    public String createPaymentIntent(UUID paymentId, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("[Cashfree] Creating order for paymentId={}, amount={} {}, idempotencyKey={}",
                paymentId, amount, currency, idempotencyKey);
        // Production: Cashfree SDK CreateOrderRequest + CFPaymentGatewayService
        return "cf_order_mock_" + paymentId.toString().replace("-", "");
    }

    @Override
    public String capturePayment(String paymentIntentId) {
        log.info("[Cashfree] Capturing payment: {}", paymentIntentId);
        // Production: Cashfree payments are auto-captured; this signals a settlement fetch
        return "cf_pay_captured_" + System.currentTimeMillis();
    }

    @Override
    public String issueRefund(String gatewayPaymentId, BigDecimal amount, String reason) {
        log.info("[Cashfree] Issuing refund for gatewayPaymentId={}, amount={}, reason={}",
                gatewayPaymentId, amount, reason);
        // Production: Cashfree Refund API via CFRefundService
        return "cf_rfnd_mock_" + System.currentTimeMillis();
    }

    @Override
    public String initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount) {
        log.info("[Cashfree] Initiating payout for ownerId={}, amount={}, destination={}",
                ownerId, amount, destinationAccount);
        // Production: Cashfree Payouts API - RequestTransfer
        return "cf_pout_mock_" + System.currentTimeMillis();
    }

    /**
     * Verifies a Cashfree webhook signature.
     *
     * <p>Cashfree computes: {@code Base64( HMAC-SHA256( <timestamp><rawBody>, clientSecret ) )}.
     * The {@code signature} parameter here must be in the format: {@code <timestamp>:<base64sig>}.
     * This is assembled by the controller before delegating here.
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            log.warn("[Cashfree] Missing payload, signature, or secret for webhook verification");
            return false;
        }
        try {
            // Expect signature format: "<timestamp>:<base64-encoded-sig>"
            int colonIdx = signature.indexOf(':');
            if (colonIdx < 0) {
                log.warn("[Cashfree] Invalid signature format (expected '<timestamp>:<base64sig>'): {}", signature);
                return false;
            }
            String timestamp = signature.substring(0, colonIdx);
            String receivedBase64Sig = signature.substring(colonIdx + 1);

            String dataToSign = timestamp + payload;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String computedBase64Sig = Base64.getEncoder().encodeToString(digest);

            boolean valid = computedBase64Sig.equals(receivedBase64Sig);
            if (!valid) {
                log.warn("[Cashfree] Webhook signature mismatch. Potential replay or tampering detected.");
            }
            return valid;
        } catch (Exception e) {
            log.error("[Cashfree] Webhook signature verification failed with exception", e);
            return false;
        }
    }
}
