package com.roomwallah.booking.presentation.dto;

import com.roomwallah.booking.domain.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private UUID id;
    private UUID propertyId;
    private UUID tenantId;
    private UUID ownerId;
    private BookingStatus status;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private String notes;
    private Instant createdAt;
}
