package com.roomwallah.payment.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRequest {

    @NotBlank(message = "Gateway provider is required")
    private String gatewayProvider;

    @NotEmpty(message = "Records list must not be empty")
    @Valid
    private List<GatewayRecordDto> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayRecordDto {

        @NotBlank(message = "Gateway transaction ID is required")
        private String gatewayTransactionId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Status is required")
        private String status;
    }
}
