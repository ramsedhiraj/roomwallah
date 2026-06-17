package com.roomwallah.media.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.media.application.facade.MediaFacade;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.port.MediaStoragePort;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.media.domain.valueobject.UploadSession;
import com.roomwallah.media.presentation.dto.MediaResponse;
import com.roomwallah.media.presentation.dto.ReorderRequest;
import com.roomwallah.media.presentation.dto.RepositionRequest;
import com.roomwallah.media.presentation.dto.StartSessionRequest;
import com.roomwallah.media.presentation.dto.UploadSessionResponse;
import com.roomwallah.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Property Media", description = "Property media & asset management endpoints")
public class MediaController {

    private final MediaFacade mediaFacade;
    private final CurrentUserProvider currentUserProvider;
    private final MediaStoragePort mediaStoragePort;
    private final PropertyMediaRepository propertyMediaRepository;

    @PostMapping("/properties/{propertyId}")
    @Operation(summary = "Upload a media file for a property")
    public ApiResponse<MediaResponse> uploadMedia(
            @PathVariable UUID propertyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws IOException {
        log.info("Received request to upload media: {} for property: {}, Idempotency-Key: {}", file.getOriginalFilename(), propertyId, idempotencyKey);
        User user = currentUserProvider.getCurrentUser();
        PropertyMedia media = mediaFacade.uploadMedia(
                propertyId,
                user.getId(),
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                mediaType,
                idempotencyKey
        );
        return ApiResponse.success(mapToResponse(media), "Media uploaded successfully");
    }

    @GetMapping("/properties/{propertyId}")
    @Operation(summary = "Get all active media for a property")
    public ApiResponse<List<MediaResponse>> getPropertyMedia(@PathVariable UUID propertyId) {
        log.info("Received request to get media for property: {}", propertyId);
        List<PropertyMedia> mediaList = mediaFacade.getPropertyMedia(propertyId);
        List<MediaResponse> responses = mediaList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses, "Media retrieved successfully");
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Soft delete a media item")
    public ApiResponse<Void> deleteMedia(@PathVariable UUID mediaId) {
        log.info("Received request to delete media: {}", mediaId);
        User user = currentUserProvider.getCurrentUser();
        mediaFacade.deleteMedia(mediaId, user.getId());
        return ApiResponse.success("Media deleted successfully");
    }

    @PatchMapping("/{mediaId}/cover")
    @Operation(summary = "Mark a media item as cover image")
    public ApiResponse<Void> setCoverImage(@PathVariable UUID mediaId) {
        log.info("Received request to set media: {} as cover", mediaId);
        User user = currentUserProvider.getCurrentUser();
        
        PropertyMedia media = propertyMediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        mediaFacade.setCoverImage(media.getPropertyId(), user.getId(), mediaId);
        return ApiResponse.success("Cover image updated successfully");
    }

    @PatchMapping("/reorder")
    @Operation(summary = "Reorder property media gallery (gap-based spacing)")
    public ApiResponse<Void> reorderMedia(@Valid @RequestBody ReorderRequest request) {
        log.info("Received request to reorder media for property: {}", request.getPropertyId());
        User user = currentUserProvider.getCurrentUser();
        mediaFacade.reorderMedia(request.getPropertyId(), user.getId(), request.getMediaIds());
        return ApiResponse.success("Media reordered successfully");
    }

    @PatchMapping("/{mediaId}/position")
    @Operation(summary = "Reposition a media item inside gallery (optimized single-row update)")
    public ApiResponse<Void> repositionMedia(
            @PathVariable UUID mediaId,
            @RequestParam("propertyId") UUID propertyId,
            @Valid @RequestBody RepositionRequest request
    ) {
        log.info("Received request to reposition media: {} for property: {}", mediaId, propertyId);
        User user = currentUserProvider.getCurrentUser();
        mediaFacade.repositionMedia(propertyId, user.getId(), mediaId, request.getPrevMediaId(), request.getNextMediaId());
        return ApiResponse.success("Media repositioned successfully");
    }

    // ------------------------------------------------------------------------
    // Upload Sessions
    // ------------------------------------------------------------------------

    @PostMapping("/sessions")
    @Operation(summary = "Start a chunked upload session")
    public ApiResponse<UploadSessionResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        log.info("Starting upload session for property: {}", request.getPropertyId());
        User user = currentUserProvider.getCurrentUser();
        UploadSession session = mediaFacade.startSession(
                request.getPropertyId(),
                user.getId(),
                request.getFilename(),
                request.getTotalSize(),
                request.getMediaType()
        );
        UploadSessionResponse response = new UploadSessionResponse(
                session.getSessionId(),
                session.getPropertyId(),
                session.getFilename(),
                session.getTotalSize(),
                session.getMediaType()
        );
        return ApiResponse.success(response, "Upload session initiated successfully");
    }

    @PostMapping("/sessions/{sessionId}/chunks")
    @Operation(summary = "Upload a chunk for a session")
    public ApiResponse<Void> uploadChunk(
            @PathVariable String sessionId,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Uploading chunk {} for session: {}", chunkNumber, sessionId);
        User user = currentUserProvider.getCurrentUser();
        mediaFacade.uploadChunk(sessionId, user.getId(), chunkNumber, file.getBytes());
        return ApiResponse.success("Chunk uploaded successfully");
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Complete an upload session and assemble chunks")
    public ApiResponse<MediaResponse> completeSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Completing upload session: {}, Idempotency-Key: {}", sessionId, idempotencyKey);
        User user = currentUserProvider.getCurrentUser();
        PropertyMedia media = mediaFacade.completeSession(sessionId, user.getId(), idempotencyKey);
        return ApiResponse.success(mapToResponse(media), "Upload session completed and media saved");
    }

    @GetMapping("/files/**")
    @Operation(summary = "Securely serve media files from storage")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String key = path.substring("/api/v1/media/files/".length());
        
        try {
            InputStream is = mediaStoragePort.retrieve(key);
            InputStreamResource resource = new InputStreamResource(is);
            
            String contentType = "application/octet-stream";
            if (key.endsWith(".png")) contentType = "image/png";
            else if (key.endsWith(".jpg") || key.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (key.endsWith(".webp")) contentType = "image/webp";
            else if (key.endsWith(".gif")) contentType = "image/gif";
            else if (key.endsWith(".mp4")) contentType = "video/mp4";
            else if (key.endsWith(".pdf")) contentType = "application/pdf";
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving file: " + key, e);
            return ResponseEntity.notFound().build();
        }
    }

    private MediaResponse mapToResponse(PropertyMedia media) {
        String relativeUrl = mediaStoragePort.generateUrl(media.getObjectKey());
        return MediaResponse.builder()
                .id(media.getId())
                .propertyId(media.getPropertyId())
                .objectKey(media.getObjectKey())
                .mediaType(media.getMediaType())
                .processingStatus(media.getProcessingStatus())
                .moderationStatus(media.getModerationStatus())
                .displayOrder((int) media.getDisplayOrder())
                .isCover(media.isCover())
                .mimeType(media.getMetadata() != null ? media.getMetadata().getMimeType() : null)
                .fileSize(media.getMetadata() != null ? media.getMetadata().getFileSize() : null)
                .width(media.getMetadata() != null ? media.getMetadata().getWidth() : null)
                .height(media.getMetadata() != null ? media.getMetadata().getHeight() : null)
                .durationSeconds(media.getMetadata() != null ? media.getMetadata().getDurationSeconds() : null)
                .url(relativeUrl)
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }
}
