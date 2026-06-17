package com.roomwallah.property.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PropertyPausedEvent {
    private final UUID propertyId;
    private final String listingRef;
    private final UUID ownerId;
    private final Instant pausedAt;
}
