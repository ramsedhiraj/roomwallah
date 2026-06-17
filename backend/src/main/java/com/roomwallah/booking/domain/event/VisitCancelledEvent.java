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
public class VisitCancelledEvent {
    private UUID visitId;
    private UUID propertyId;
    private UUID tenantId;
    private UUID visitSlotId;
    private String cancelledBy;
    private Instant cancelledAt;
}
