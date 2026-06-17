package com.roomwallah.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private UUID id;
    private UUID bookingId;
    private UUID tenantId;
    private UUID ownerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String gatewayProvider;
    private String gatewayPaymentId;
    private String idempotencyKey;
    private Integer riskScore;
    private String riskDecision;
}
