package com.roomwallah.booking.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private UUID bookingId;
    private UUID propertyId;
    private UUID tenantId;
    private UUID ownerId;
    private String cancelledBy; // e.g. "TENANT" or "OWNER"
    private Instant cancelledAt;
}
