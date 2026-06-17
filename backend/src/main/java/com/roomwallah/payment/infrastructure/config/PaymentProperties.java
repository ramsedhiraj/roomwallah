package com.roomwallah.payment.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for the Payment bounded context.
 *
 * <p>Bound from {@code roomwallah.payment.*} in application.yml.
 * All gateway credentials should be overridden via environment variables in production.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "roomwallah.payment")
public class PaymentProperties {

    /** Master feature flag. When false, no payment processing occurs. */
    private boolean paymentsEnabled = true;

    /** Controls escrow fund hold/release flows. */
    private boolean escrowEnabled = true;

    /** Controls wallet top-up/withdraw flows (experimental). */
    private boolean walletEnabled = false;

    /** Controls dispute creation and resolution flows. */
    private boolean disputesEnabled = true;

    private GatewayProperties gateway = new GatewayProperties();
    private WebhookProperties webhook = new WebhookProperties();

    @Getter
    @Setter
    public static class GatewayProperties {
        /** The active payment gateway provider. One of: STRIPE, RAZORPAY, CASHFREE. */
        private String activeProvider = "STRIPE";

        private String stripeSecretKey = "sk_test_placeholder";

        private String razorpayKeyId = "rzp_test_placeholder";
        private String razorpayKeySecret = "rzp_test_secret_placeholder";

        private String cashfreeClientId = "cf_test_placeholder";
        private String cashfreeClientSecret = "cf_test_secret_placeholder";
    }

    @Getter
    @Setter
    public static class WebhookProperties {
        /** Stripe webhook signing secret (whsec_... from Stripe dashboard). */
        private String stripeSecret = "whsec_stripe_placeholder";

        /** Razorpay webhook secret set in the Razorpay dashboard. */
        private String razorpaySecret = "whsec_razorpay_placeholder";

        /** Cashfree client secret used to verify webhook signatures. */
        private String cashfreeSecret = "whsec_cashfree_placeholder";
    }
}
