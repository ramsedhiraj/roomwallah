package com.roomwallah.media.presentation.dto;

import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.ProcessingStatus;
import com.roomwallah.media.domain.entity.ModerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MediaResponse {
    private final UUID id;
    private final UUID propertyId;
    private final String objectKey;
    private final MediaType mediaType;
    private final ProcessingStatus processingStatus;
    private final ModerationStatus moderationStatus;
    private final int displayOrder;
    private final boolean isCover;
    private final String mimeType;
    private final Long fileSize;
    private final Integer width;
    private final Integer height;
    private final Integer durationSeconds;
    private final String url;
    private final Instant createdAt;
    private final Instant updatedAt;
}
