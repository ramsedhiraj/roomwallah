package com.roomwallah.booking.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    private UUID bookingId;
    private UUID propertyId;
    private UUID tenantId;
    private UUID ownerId;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private String notes;
    private Instant createdAt;
}
