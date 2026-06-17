package com.roomwallah.media.domain.event;

import com.roomwallah.media.domain.entity.ProcessingStatus;
import com.roomwallah.media.domain.entity.ModerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MediaProcessingCompletedEvent {
    private final UUID mediaId;
    private final UUID propertyId;
    private final ProcessingStatus processingStatus;
    private final ModerationStatus moderationStatus;
    private final Instant updatedAt;
}
