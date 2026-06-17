package com.roomwallah.media.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ThumbnailGeneratedEvent {
    private final UUID mediaId;
    private final UUID propertyId;
    private final String thumbnailKey;
    private final Instant generatedAt;
}
