package com.roomwallah.booking.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadRequestDto {
    
    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    private String inquiryText;

    private String contactPhone;

    private String contactEmail;
}
