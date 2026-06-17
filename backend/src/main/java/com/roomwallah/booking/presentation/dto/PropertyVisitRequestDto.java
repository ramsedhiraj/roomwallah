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
public class PropertyVisitRequestDto {

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Visit slot ID is required")
    private UUID visitSlotId;

    private String notes;
}
