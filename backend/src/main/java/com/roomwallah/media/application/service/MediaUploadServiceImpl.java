package com.roomwallah.media.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.ModerationStatus;
import com.roomwallah.media.domain.entity.ProcessingStatus;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.entity.UploadIdempotency;
import com.roomwallah.media.domain.port.MediaPolicyPort;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.port.UploadSessionPort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.repository.UploadIdempotencyRepository;
import com.roomwallah.media.domain.valueobject.FileChecksum;
import com.roomwallah.media.domain.valueobject.MediaMetadata;
import com.roomwallah.media.domain.valueobject.UploadSession;
import com.roomwallah.media.domain.event.MediaUploadedEvent;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final UploadIdempotencyRepository uploadIdempotencyRepository;
    private final MediaStoragePort mediaStoragePort;
    private final MediaPolicyPort mediaPolicyPort;
    private final UploadSessionPort uploadSessionPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public PropertyMedia upload(UUID propertyId, UUID ownerId, byte[] content, String originalFilename, String mimeType, MediaType mediaType, String idempotencyKey) {
        // 1. Check idempotency key first
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<UploadIdempotency> existing = uploadIdempotencyRepository.findById(idempotencyKey);
            if (existing.isPresent()) {
                // If it already exists, return the previously created media item
                Optional<PropertyMedia> media = propertyMediaRepository.findByIdAndDeletedFalse(existing.get().getMediaId());
                if (media.isPresent()) {
                    return media.get();
                }
            }
        }

        // 2. Verify Property ownership
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // 3. Validate using policy
        if (!mediaPolicyPort.isSupportedMimeType(mediaType, mimeType)) {
            throw new IllegalArgumentException("MIME type not supported for media type " + mediaType);
        }

        String extension = getFileExtension(originalFilename);
        if (!mediaPolicyPort.isSupportedExtension(mediaType, extension)) {
            throw new IllegalArgumentException("File extension not supported: " + extension);
        }

        if (content.length > mediaPolicyPort.getMaxFileSize(mediaType)) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }

        // 4. Validate duplicate uploads using SHA-256 checksum
        String checksumHex = calculateSha256(content);
        if (propertyMediaRepository.existsByPropertyIdAndChecksumChecksumSha256AndDeletedFalse(propertyId, checksumHex)) {
            throw new IllegalArgumentException("Duplicate upload detected: file already uploaded for this property");
        }

        // 5. Validate configured media limits
        long currentCount = propertyMediaRepository.countByPropertyIdAndMediaTypeAndDeletedFalse(propertyId, mediaType);
        int maxLimit = getLimitForType(mediaType);
        if (currentCount >= maxLimit) {
            throw new IllegalArgumentException("Upload limit exceeded for media type: " + mediaType);
        }

        // 6. Generate unique object key
        String uniqueId = UUID.randomUUID().toString();
        String cleanFilename = originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "unnamed";
        String objectKey = "properties/" + propertyId + "/" + mediaType.name().toLowerCase() + "/" + uniqueId + "-" + cleanFilename;

        // 7. Save in storage
        mediaStoragePort.store(objectKey, new ByteArrayInputStream(content), mimeType, content.length);

        // 8. Persist database record
        PropertyMedia media = new PropertyMedia();
        media.setPropertyId(propertyId);
        media.setObjectKey(objectKey);
        media.setMediaType(mediaType);
        media.setProcessingStatus(ProcessingStatus.PENDING);
        media.setModerationStatus(ModerationStatus.PENDING);
        media.setCover(false);

        // Gap-based initial display order
        List<PropertyMedia> existingMedia = propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId);
        long nextOrder = existingMedia.isEmpty() ? 1000L : existingMedia.get(existingMedia.size() - 1).getDisplayOrder() + 1000L;
        media.setDisplayOrder(nextOrder);

        // Audit information
        media.setRevision(1);
        media.setUploadedAt(Instant.now());
        media.setStorageProvider("LOCAL");

        // Metadata & checksum
        media.setMetadata(new MediaMetadata(mimeType, (long) content.length, null, null, null));
        media.setChecksum(new FileChecksum(checksumHex));

        PropertyMedia saved = propertyMediaRepository.save(media);

        // If idempotency key was supplied, persist it
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            uploadIdempotencyRepository.save(new UploadIdempotency(idempotencyKey, saved.getId(), Instant.now()));
        }

        // 9. Publish MediaUploadedEvent
        eventPublisherPort.publish(MediaUploadedEvent.builder()
                .mediaId(saved.getId())
                .propertyId(saved.getPropertyId())
                .objectKey(saved.getObjectKey())
                .mediaType(saved.getMediaType())
                .checksum(saved.getChecksum().getChecksumSha256())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now())
                .build());

        return saved;
    }

    @Override
    @Transactional
    public UploadSession startSession(UUID propertyId, UUID ownerId, String filename, long totalSize, MediaType mediaType) {
        // Verify Property ownership
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // Verify total size policy limits early
        if (totalSize > mediaPolicyPort.getMaxFileSize(mediaType)) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + mediaPolicyPort.getMaxFileSize(mediaType) + " bytes");
        }

        return uploadSessionPort.createSession(propertyId, filename, totalSize, mediaType);
    }

    @Override
    @Transactional
    public void uploadChunk(String sessionId, UUID ownerId, int chunkNumber, byte[] chunkData) {
        UploadSession session = uploadSessionPort.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Upload session not found: " + sessionId);
        }

        // Verify ownership
        Property property = propertyRepository.findById(session.getPropertyId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        uploadSessionPort.uploadChunk(sessionId, chunkNumber, chunkData);
    }

    @Override
    @Transactional
    public PropertyMedia completeSession(String sessionId, UUID ownerId, String idempotencyKey) {
        UploadSession session = uploadSessionPort.getSession(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Upload session not found: " + sessionId);
        }

        // Verify ownership
        Property property = propertyRepository.findById(session.getPropertyId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // Assemble chunks
        byte[] assembledBytes = uploadSessionPort.assembleSession(sessionId);

        // Guess MIME type
        String mimeType = guessMimeType(session.getFilename());

        // Call the upload logic
        PropertyMedia media = upload(session.getPropertyId(), ownerId, assembledBytes, session.getFilename(), mimeType, session.getMediaType(), idempotencyKey);

        // Clear session chunks
        uploadSessionPort.deleteSession(sessionId);

        return media;
    }

    private String calculateSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256 checksum", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String guessMimeType(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        switch (ext) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "webp": return "image/webp";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "ogg": return "video/ogg";
            case "pdf": return "application/pdf";
            default: return "application/octet-stream";
        }
    }

    private int getLimitForType(MediaType type) {
        switch (type) {
            case IMAGE:
                return mediaPolicyPort.getMaxImagesPerProperty();
            case VIDEO:
                return mediaPolicyPort.getMaxVideosPerProperty();
            case FLOOR_PLAN:
                return mediaPolicyPort.getMaxFloorPlansPerProperty();
            case VIRTUAL_TOUR:
                return mediaPolicyPort.getMaxVirtualToursPerProperty();
            default:
                return 1;
        }
    }
}
