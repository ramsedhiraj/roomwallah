package com.roomwallah.media.domain.valueobject;

import com.roomwallah.media.domain.entity.MediaType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class UploadSession {
    private final String sessionId;
    private final UUID propertyId;
    private final String filename;
    private final long totalSize;
    private final MediaType mediaType;
}
