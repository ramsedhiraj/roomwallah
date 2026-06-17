package com.roomwallah.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponseDto {
    private UUID id;
    private String gatewayProvider;
    private String eventType;
    private String payloadJson;
    private boolean processed;
    private Instant processedAt;
    private String errorReason;
    private Instant createdAt;
    private Instant updatedAt;
}
