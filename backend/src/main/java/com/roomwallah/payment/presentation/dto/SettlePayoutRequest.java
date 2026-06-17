package com.roomwallah.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlePayoutRequest {

    @NotBlank(message = "Gateway payout ID is required")
    private String gatewayPayoutId;
}
