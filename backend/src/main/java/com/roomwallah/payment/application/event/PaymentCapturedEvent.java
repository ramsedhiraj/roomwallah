package com.roomwallah.payment.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PaymentCapturedEvent {
    private final UUID paymentId;
    private final UUID bookingId;
    private final UUID tenantId;
    private final UUID ownerId;
    private final BigDecimal amount;
    private final String currency;
    private final String gatewayPaymentId;
    private final Instant capturedAt;
}
