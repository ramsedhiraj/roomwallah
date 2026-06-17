package com.roomwallah.media.application.service;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.valueobject.UploadSession;

import java.util.UUID;

public interface MediaUploadService {
    PropertyMedia upload(UUID propertyId, UUID ownerId, byte[] content, String originalFilename, String mimeType, MediaType mediaType, String idempotencyKey);
    UploadSession startSession(UUID propertyId, UUID ownerId, String filename, long totalSize, MediaType mediaType);
    void uploadChunk(String sessionId, UUID ownerId, int chunkNumber, byte[] chunkData);
    PropertyMedia completeSession(String sessionId, UUID ownerId, String idempotencyKey);
}
