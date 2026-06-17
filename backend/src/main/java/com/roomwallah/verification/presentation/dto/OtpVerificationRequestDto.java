package com.roomwallah.verification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerificationRequestDto {
    @NotBlank(message = "OTP code is mandatory")
    private String code;
}
