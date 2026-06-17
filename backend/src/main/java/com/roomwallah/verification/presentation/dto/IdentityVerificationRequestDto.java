package com.roomwallah.verification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdentityVerificationRequestDto {
    @NotBlank(message = "Provider is mandatory")
    private String provider;

    @NotBlank(message = "Code is mandatory")
    private String code;
}
