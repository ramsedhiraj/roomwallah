package com.roomwallah.booking.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Price amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price amount must be greater than zero")
    private BigDecimal priceAmount;

    @NotBlank(message = "Price currency is required")
    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String priceCurrency;

    private String notes;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
