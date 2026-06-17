package com.roomwallah.media.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.domain.entity.MediaDerivative;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.ModerationStatus;
import com.roomwallah.media.domain.entity.ProcessingStatus;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.port.ImageOptimizerPort;
import com.roomwallah.media.domain.port.MediaModerationPort;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.port.ThumbnailGeneratorPort;
import com.roomwallah.media.domain.port.VirusScannerPort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.valueobject.MediaMetadata;
import com.roomwallah.media.domain.event.MediaProcessingCompletedEvent;
import com.roomwallah.media.domain.event.ThumbnailGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private final PropertyMediaRepository propertyMediaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final VirusScannerPort virusScannerPort;
    private final ImageOptimizerPort imageOptimizerPort;
    private final ThumbnailGeneratorPort thumbnailGeneratorPort;
    private final MediaModerationPort mediaModerationPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public void processMedia(UUID mediaId) {
        PropertyMedia media = propertyMediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        if (media.getProcessingStatus() == ProcessingStatus.READY || media.getProcessingStatus() == ProcessingStatus.FAILED) {
            return;
        }

        media.setProcessingStatus(ProcessingStatus.PROCESSING);
        media = propertyMediaRepository.save(media);

        try {
            // Retrieve file bytes
            byte[] fileBytes;
            try (InputStream is = mediaStoragePort.retrieve(media.getObjectKey())) {
                fileBytes = readAllBytes(is);
            }

            // 1. Virus scan
            if (!virusScannerPort.scan(fileBytes)) {
                media.setModeratedAt(Instant.now());
                throw new SecurityException("Virus detected in uploaded media");
            }
            media.setScannedAt(Instant.now());

            byte[] currentBytes = fileBytes;
            Integer width = null;
            Integer height = null;
            Integer duration = null;

            // 2. EXIF metadata stripping & Image Optimization
            if (media.getMediaType() == MediaType.IMAGE) {
                // EXIF stripping via ImageIO rewrite
                currentBytes = stripExif(currentBytes, media.getMetadata().getMimeType());
                media.setOptimizedAt(Instant.now());
                
                // Optimize
                currentBytes = imageOptimizerPort.optimize(currentBytes, media.getMetadata().getMimeType());
                width = 1920; 
                height = 1080; 
            } else if (media.getMediaType() == MediaType.VIDEO) {
                media.setOptimizedAt(Instant.now());
                duration = 120; 
            }

            // 3. Thumbnail Generation
            if (media.getMediaType() == MediaType.IMAGE) {
                byte[] thumbBytes = thumbnailGeneratorPort.generateThumbnail(currentBytes, media.getMetadata().getMimeType());
                String thumbKey = media.getObjectKey() + "-thumb";
                mediaStoragePort.store(thumbKey, new ByteArrayInputStream(thumbBytes), media.getMetadata().getMimeType(), thumbBytes.length);
                media.setThumbnailGeneratedAt(Instant.now());

                // Save derivative asset in the relationship
                MediaDerivative derivative = new MediaDerivative();
                derivative.setMediaId(media.getId());
                derivative.setVariantType("THUMBNAIL");
                derivative.setObjectKey(thumbKey);
                derivative.setMimeType(media.getMetadata().getMimeType());
                derivative.setFileSize((long) thumbBytes.length);
                derivative.setWidth(200);
                derivative.setHeight(200);
                media.getDerivatives().add(derivative);

                eventPublisherPort.publish(ThumbnailGeneratedEvent.builder()
                        .mediaId(media.getId())
                        .propertyId(media.getPropertyId())
                        .thumbnailKey(thumbKey)
                        .generatedAt(Instant.now())
                        .build());
            }

            // 4. Content Moderation
            if (!mediaModerationPort.scanContent(currentBytes, media.getMetadata().getMimeType())) {
                throw new IllegalArgumentException("Inappropriate content detected");
            }
            media.setModeratedAt(Instant.now());

            // Pipeline successful: update status
            media.setProcessingStatus(ProcessingStatus.READY);
            media.setModerationStatus(ModerationStatus.APPROVED);
            media.setReadyAt(Instant.now());
            
            // Set metadata
            media.setMetadata(new MediaMetadata(
                    media.getMetadata().getMimeType(),
                    (long) currentBytes.length,
                    width,
                    height,
                    duration
            ));

            PropertyMedia saved = propertyMediaRepository.save(media);

            eventPublisherPort.publish(MediaProcessingCompletedEvent.builder()
                    .mediaId(saved.getId())
                    .propertyId(saved.getPropertyId())
                    .processingStatus(saved.getProcessingStatus())
                    .moderationStatus(saved.getModerationStatus())
                    .updatedAt(Instant.now())
                    .build());

        } catch (Exception e) {
            log.error("Failed to process media with ID: " + mediaId, e);
            
            media.setProcessingStatus(ProcessingStatus.FAILED);
            media.setModerationStatus(ModerationStatus.REJECTED);
            PropertyMedia saved = propertyMediaRepository.save(media);

            eventPublisherPort.publish(MediaProcessingCompletedEvent.builder()
                    .mediaId(saved.getId())
                    .propertyId(saved.getPropertyId())
                    .processingStatus(saved.getProcessingStatus())
                    .moderationStatus(saved.getModerationStatus())
                    .updatedAt(Instant.now())
                    .build());
        }
    }

    private byte[] stripExif(byte[] content, String mimeType) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(content);
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                return content;
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String format = mimeType.replace("image/", "");
            if ("jpeg".equalsIgnoreCase(format)) {
                format = "jpg";
            }
            
            boolean success = ImageIO.write(img, format, out);
            return success ? out.toByteArray() : content;
        } catch (Exception e) {
            log.warn("Failed to strip EXIF metadata from image. Using fallback bytes.", e);
            return content;
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
