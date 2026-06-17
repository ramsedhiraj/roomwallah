package com.roomwallah.property.presentation.dto;

import com.roomwallah.property.domain.entity.AreaUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaMeasurementDto {
    private BigDecimal value;
    private AreaUnit unit;
}
