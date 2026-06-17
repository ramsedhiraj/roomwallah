package com.roomwallah.verification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDecisionRequestDto {
    @NotBlank(message = "Reason is mandatory for administrative decisions")
    private String reason;
}
