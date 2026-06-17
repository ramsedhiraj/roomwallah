package com.roomwallah.media.presentation.dto;

import com.roomwallah.media.domain.entity.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UploadSessionResponse {
    private final String sessionId;
    private final UUID propertyId;
    private final String filename;
    private final long totalSize;
    private final MediaType mediaType;
}
