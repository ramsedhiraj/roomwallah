package com.roomwallah.media.application.facade;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.valueobject.UploadSession;

import java.util.List;
import java.util.UUID;

public interface MediaFacade {
    PropertyMedia uploadMedia(UUID propertyId, UUID callerId, byte[] content, String originalFilename, String mimeType, MediaType mediaType, String idempotencyKey);
    List<PropertyMedia> getPropertyMedia(UUID propertyId);
    void deleteMedia(UUID mediaId, UUID callerId);
    void setCoverImage(UUID propertyId, UUID callerId, UUID mediaId);
    void reorderMedia(UUID propertyId, UUID callerId, List<UUID> mediaIds);
    
    // Hardening extensions
    void repositionMedia(UUID propertyId, UUID callerId, UUID mediaId, UUID prevMediaId, UUID nextMediaId);
    UploadSession startSession(UUID propertyId, UUID callerId, String filename, long totalSize, MediaType mediaType);
    void uploadChunk(String sessionId, UUID callerId, int chunkNumber, byte[] chunkData);
    PropertyMedia completeSession(String sessionId, UUID callerId, String idempotencyKey);
}
