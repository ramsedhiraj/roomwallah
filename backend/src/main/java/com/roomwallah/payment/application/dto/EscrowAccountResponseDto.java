package com.roomwallah.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowAccountResponseDto {
    private UUID id;
    private UUID bookingId;
    private UUID paymentId;
    private UUID tenantId;
    private UUID ownerId;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant heldAt;
    private Instant releasedAt;
}
