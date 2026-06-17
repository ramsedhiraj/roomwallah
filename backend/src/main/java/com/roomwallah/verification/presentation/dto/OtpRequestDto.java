package com.roomwallah.verification.presentation.dto;

import lombok.Data;

@Data
public class OtpRequestDto {
    private String target; // Optional: can be email or phone to override profile
}
