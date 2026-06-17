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
public class DisputeResponseDto {
    private UUID id;
    private UUID paymentId;
    private String reason;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String evidenceJson;
    private Instant createdAt;
    private Instant updatedAt;
}
