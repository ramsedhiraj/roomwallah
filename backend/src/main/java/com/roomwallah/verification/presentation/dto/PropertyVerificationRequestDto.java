package com.roomwallah.verification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PropertyVerificationRequestDto {
    @NotNull(message = "Property ID is mandatory")
    private UUID propertyId;

    @NotBlank(message = "Document URL is mandatory")
    private String documentUrl;

    @NotBlank(message = "Utility bill URL is mandatory")
    private String utilityBillUrl;

    private String ownerNameOnDeed;
    private String addressOnUtilityBill;
    private String ownerNameOnUtilityBill;
}
