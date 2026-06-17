package com.roomwallah.user.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.ObjectStoragePort;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.event.AvatarUpdatedEvent;
import com.roomwallah.user.presentation.dto.AvatarUploadResponse;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService {

    private final UserRepository userRepository;
    private final ObjectStoragePort objectStoragePort;
    private final EventPublisherPort eventPublisher;
    private final AuditPort auditPort;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(User user, InputStream inputStream, String originalFileName, String contentType) {
        log.info("Processing avatar upload request for user: {}", user.getEmail());

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported image type. Only JPEG, PNG and WEBP are allowed.");
        }

        String extension = getFileExtension(originalFileName);
        String objectKey = "avatars/" + user.getId().toString() + "/avatar_" + System.currentTimeMillis() + extension;

        // Upload new file
        objectStoragePort.uploadFile(inputStream, objectKey, contentType);

        String oldAvatarKey = user.getAvatarKey();
        if (oldAvatarKey != null && !oldAvatarKey.isBlank() && !oldAvatarKey.equals(objectKey)) {
            // Refinement: Avoid permanently deleting old avatar objects during replacement. Prefer marking them for asynchronous cleanup.
            log.info("Marking old avatar key [{}] for asynchronous cleanup.", oldAvatarKey);
            auditPort.log(
                    "AVATAR_CLEANUP_PENDING",
                    user.getId().toString(),
                    "0.0.0.0",
                    Map.of("objectKey", oldAvatarKey)
            );
        }

        user.setAvatarKey(objectKey);
        userRepository.save(user);

        String newPublicUrl = objectStoragePort.getPublicUrl(objectKey);

        // Publish event
        AvatarUpdatedEvent event = AvatarUpdatedEvent.builder()
                .userId(user.getId())
                .avatarKey(objectKey)
                .updatedAt(Instant.now())
                .build();
        eventPublisher.publish(event);

        // Audit log
        auditPort.log(
                "USER_AVATAR_UPDATE",
                user.getId().toString(),
                "0.0.0.0",
                Map.of(
                        "email", user.getEmail(),
                        "avatarKey", objectKey
                )
        );

        return AvatarUploadResponse.builder()
                .avatarKey(objectKey)
                .avatarUrl(newPublicUrl)
                .build();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".webp"; // Default fallback
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
