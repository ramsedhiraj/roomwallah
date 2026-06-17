package com.roomwallah.media.application.service;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.port.UploadSessionPort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.repository.UploadIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaCleanupJob {

    private final UploadSessionPort uploadSessionPort;
    private final UploadIdempotencyRepository uploadIdempotencyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final MediaStoragePort mediaStoragePort;

    // Run every hour
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void runCleanup() {
        log.info("Starting scheduled media cleanup job...");

        // 1. Stale Idempotency keys (older than 7 days)
        try {
            Instant idempotencyCutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            uploadIdempotencyRepository.deleteOlderThan(idempotencyCutoff);
            log.info("Successfully cleaned up stale idempotency records.");
        } catch (Exception e) {
            log.error("Failed to clean up stale idempotency records", e);
        }

        // 2. Expired upload sessions (older than 24 hours)
        try {
            Instant sessionCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
            List<String> expiredSessionIds = uploadSessionPort.getExpiredSessionIds(sessionCutoff);
            for (String sessionId : expiredSessionIds) {
                uploadSessionPort.deleteSession(sessionId);
            }
            log.info("Successfully cleaned up {} expired upload sessions.", expiredSessionIds.size());
        } catch (Exception e) {
            log.error("Failed to clean up expired upload sessions", e);
        }

        // 3. Deferred physical file deletion (soft-deleted media older than 24 hours)
        try {
            Instant deleteCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
            List<PropertyMedia> toDelete = propertyMediaRepository.findByDeletedTrueAndDeletedAtBefore(deleteCutoff);
            for (PropertyMedia media : toDelete) {
                try {
                    // Try deleting main file
                    mediaStoragePort.delete(media.getObjectKey());
                    // Try deleting thumbnail
                    mediaStoragePort.delete(media.getObjectKey() + "-thumb");
                    
                    // Also delete all derivatives associated
                    media.getDerivatives().forEach(derivative -> {
                        try {
                            mediaStoragePort.delete(derivative.getObjectKey());
                        } catch (Exception ex) {
                            log.error("Failed to delete derivative file: " + derivative.getObjectKey(), ex);
                        }
                    });

                    // Remove DB metadata record permanently
                    propertyMediaRepository.delete(media);
                    log.info("Permanently deleted storage and metadata for media: {}", media.getId());
                } catch (Exception e) {
                    log.error("Failed to clean up storage for soft-deleted media " + media.getId() + ". Will retry in next run.", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query and process soft-deleted media files", e);
        }
    }
}
