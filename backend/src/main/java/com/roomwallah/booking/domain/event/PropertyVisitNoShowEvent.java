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
public class PropertyVisitNoShowEvent {
    private UUID visitId;
    private UUID tenantId;
}
