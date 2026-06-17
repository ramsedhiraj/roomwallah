package com.roomwallah.media.infrastructure.event;

import com.roomwallah.media.application.service.MediaProcessingService;
import com.roomwallah.media.domain.event.MediaDeletedEvent;
import com.roomwallah.media.domain.event.MediaUploadedEvent;
import com.roomwallah.media.domain.port.MediaStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaEventListener {

    private final MediaProcessingService mediaProcessingService;
    private final MediaStoragePort mediaStoragePort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaUploaded(MediaUploadedEvent event) {
        log.info("Media uploaded event received for media: {}", event.getMediaId());
        try {
            mediaProcessingService.processMedia(event.getMediaId());
        } catch (Exception e) {
            log.error("Error triggering media processing for media: " + event.getMediaId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaDeleted(MediaDeletedEvent event) {
        log.info("Media deleted event received for media: {}. Scheduling file cleanup.", event.getMediaId());
        try {
            // Delete main object
            mediaStoragePort.delete(event.getObjectKey());
            // Attempt to delete thumbnail if any
            mediaStoragePort.delete(event.getObjectKey() + "-thumb");
        } catch (Exception e) {
            log.error("Failed to delete physical files for media: " + event.getMediaId(), e);
        }
    }
}
