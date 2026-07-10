package com.roomwallah.property.presentation.dto;

import com.roomwallah.property.domain.entity.AreaUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaMeasurementDto {
    @NotNull(message = "Area value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Area value must be greater than zero")
    private BigDecimal value;

    @NotNull(message = "Area unit is required")
    private AreaUnit unit;
}
