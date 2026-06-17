package com.roomwallah.media.domain.event;

import com.roomwallah.media.domain.entity.MediaType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MediaUploadedEvent {
    private final UUID mediaId;
    private final UUID propertyId;
    private final String objectKey;
    private final MediaType mediaType;
    private final String checksum;
    private final Instant createdAt;
}
