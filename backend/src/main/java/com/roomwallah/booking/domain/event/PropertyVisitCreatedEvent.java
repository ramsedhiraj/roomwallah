package com.roomwallah.booking.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyVisitCreatedEvent {
    private UUID visitId;
    private UUID propertyId;
    private UUID tenantId;
    private UUID visitSlotId;
}
