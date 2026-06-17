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
 * Stripe payment gateway adapter.
 *
 * <p>Production integration replaces the stub return values with real Stripe SDK calls:
 * {@code com.stripe.model.PaymentIntent.create(params)} etc.
 * Signature verification uses Stripe's HMAC-SHA256 header format:
 * {@code t=<timestamp>,v1=<hex-digest>}.
 */
@Slf4j
@Component
public class StripeGatewayAdapter implements PaymentGatewayPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public String providerName() {
        return "STRIPE";
    }

    @Override
    public String createPaymentIntent(UUID paymentId, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("[Stripe] Creating payment intent for paymentId={}, amount={} {}, idempotencyKey={}",
                paymentId, amount, currency, idempotencyKey);
        // Production: Stripe.apiKey = stripeSecretKey; PaymentIntent.create(params, RequestOptions.builder().setIdempotencyKey(idempotencyKey).build())
        return "pi_stripe_mock_" + paymentId.toString().replace("-", "");
    }

    @Override
    public String capturePayment(String paymentIntentId) {
        log.info("[Stripe] Capturing payment intent: {}", paymentIntentId);
        // Production: PaymentIntent.retrieve(paymentIntentId).capture()
        return "ch_stripe_mock_captured_" + System.currentTimeMillis();
    }

    @Override
    public String issueRefund(String gatewayPaymentId, BigDecimal amount, String reason) {
        log.info("[Stripe] Issuing refund for gatewayPaymentId={}, amount={}, reason={}",
                gatewayPaymentId, amount, reason);
        // Production: Refund.create(params) where params include charge, amount, reason
        return "re_stripe_mock_" + System.currentTimeMillis();
    }

    @Override
    public String initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount) {
        log.info("[Stripe] Initiating payout for ownerId={}, amount={}, destination={}",
                ownerId, amount, destinationAccount);
        // Production: Payout.create(params) or Transfer.create(params) for connected accounts
        return "po_stripe_mock_" + System.currentTimeMillis();
    }

    /**
     * Verifies a Stripe webhook signature.
     *
     * <p>Stripe sends the header as {@code t=<timestamp>,v1=<hex-signature>}.
     * The signed payload is {@code <timestamp>.<rawBody>}.
     * This implementation verifies by recomputing HMAC-SHA256 with the webhook secret.
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            log.warn("[Stripe] Missing payload, signature, or secret for webhook verification");
            return false;
        }
        try {
            // Parse Stripe header: "t=<timestamp>,v1=<sig>"
            String timestamp = null;
            String v1Sig = null;
            for (String part : signature.split(",")) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    v1Sig = part.substring(3);
                }
            }

            if (timestamp == null || v1Sig == null) {
                log.warn("[Stripe] Invalid signature header format: {}", signature);
                return false;
            }

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(digest);

            boolean valid = computed.equalsIgnoreCase(v1Sig);
            if (!valid) {
                log.warn("[Stripe] Webhook signature mismatch. Header sig vs computed sig differ.");
            }
            return valid;
        } catch (Exception e) {
            log.error("[Stripe] Webhook signature verification failed with exception", e);
            return false;
        }
    }
}
