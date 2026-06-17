package com.roomwallah.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailPaymentRequest {

    @NotBlank(message = "Error reason is required")
    private String errorReason;
}
