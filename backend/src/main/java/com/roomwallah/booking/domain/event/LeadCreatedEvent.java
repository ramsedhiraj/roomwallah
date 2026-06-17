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
public class LeadCreatedEvent {
    private UUID leadId;
    private UUID propertyId;
    private UUID tenantId;
}
