package com.roomwallah.media.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CoverImageChangedEvent {
    private final UUID propertyId;
    private final UUID coverMediaId;
    private final UUID previousCoverMediaId;
    private final Instant updatedAt;
}
