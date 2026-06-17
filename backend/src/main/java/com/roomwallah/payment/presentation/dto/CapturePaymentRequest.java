package com.roomwallah.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapturePaymentRequest {

    @NotBlank(message = "Gateway payment ID is required")
    private String gatewayPaymentId;
}
