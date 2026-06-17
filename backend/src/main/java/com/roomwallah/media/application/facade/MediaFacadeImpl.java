package com.roomwallah.media.application.facade;

import com.roomwallah.media.application.service.*;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.valueobject.UploadSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaFacadeImpl implements MediaFacade {

    private final MediaUploadService mediaUploadService;
    private final MediaOrderingService mediaOrderingService;
    private final CoverImageService coverImageService;
    private final MediaDeletionService mediaDeletionService;
    private final PropertyMediaRepository propertyMediaRepository;

    @Override
    @Transactional
    public PropertyMedia uploadMedia(UUID propertyId, UUID callerId, byte[] content, String originalFilename, String mimeType, MediaType mediaType, String idempotencyKey) {
        return mediaUploadService.upload(propertyId, callerId, content, originalFilename, mimeType, mediaType, idempotencyKey);
    }

    @Override
    public List<PropertyMedia> getPropertyMedia(UUID propertyId) {
        return propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId, UUID callerId) {
        mediaDeletionService.delete(mediaId, callerId);
    }

    @Override
    @Transactional
    public void setCoverImage(UUID propertyId, UUID callerId, UUID mediaId) {
        coverImageService.setCoverImage(propertyId, callerId, mediaId);
    }

    @Override
    @Transactional
    public void reorderMedia(UUID propertyId, UUID callerId, List<UUID> mediaIds) {
        mediaOrderingService.reorder(propertyId, callerId, mediaIds);
    }

    @Override
    @Transactional
    public void repositionMedia(UUID propertyId, UUID callerId, UUID mediaId, UUID prevMediaId, UUID nextMediaId) {
        mediaOrderingService.reposition(propertyId, callerId, mediaId, prevMediaId, nextMediaId);
    }

    @Override
    @Transactional
    public UploadSession startSession(UUID propertyId, UUID callerId, String filename, long totalSize, MediaType mediaType) {
        return mediaUploadService.startSession(propertyId, callerId, filename, totalSize, mediaType);
    }

    @Override
    @Transactional
    public void uploadChunk(String sessionId, UUID callerId, int chunkNumber, byte[] chunkData) {
        mediaUploadService.uploadChunk(sessionId, callerId, chunkNumber, chunkData);
    }

    @Override
    @Transactional
    public PropertyMedia completeSession(String sessionId, UUID callerId, String idempotencyKey) {
        return mediaUploadService.completeSession(sessionId, callerId, idempotencyKey);
    }
}
