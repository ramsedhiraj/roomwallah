package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.port.MediaPolicyPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultMediaPolicyAdapter implements MediaPolicyPort {

    private final Map<MediaType, Set<String>> allowedMimeTypes = new HashMap<>();
    private final Map<MediaType, Set<String>> allowedExtensions = new HashMap<>();
    private final Map<MediaType, Long> maxFileSizes = new HashMap<>();

    public DefaultMediaPolicyAdapter() {
        // IMAGE configurations
        allowedMimeTypes.put(MediaType.IMAGE, Set.of("image/jpeg", "image/png", "image/webp", "image/gif"));
        allowedExtensions.put(MediaType.IMAGE, Set.of("jpg", "jpeg", "png", "webp", "gif"));
        maxFileSizes.put(MediaType.IMAGE, 10 * 1024 * 1024L); // 10MB

        // VIDEO configurations
        allowedMimeTypes.put(MediaType.VIDEO, Set.of("video/mp4", "video/webm", "video/mpeg", "video/ogg"));
        allowedExtensions.put(MediaType.VIDEO, Set.of("mp4", "webm", "mpeg", "mpg", "ogg"));
        maxFileSizes.put(MediaType.VIDEO, 100 * 1024 * 1024L); // 100MB

        // FLOOR_PLAN configurations
        allowedMimeTypes.put(MediaType.FLOOR_PLAN, Set.of("application/pdf", "image/jpeg", "image/png", "image/webp"));
        allowedExtensions.put(MediaType.FLOOR_PLAN, Set.of("pdf", "jpg", "jpeg", "png", "webp"));
        maxFileSizes.put(MediaType.FLOOR_PLAN, 20 * 1024 * 1024L); // 20MB

        // VIRTUAL_TOUR configurations
        allowedMimeTypes.put(MediaType.VIRTUAL_TOUR, Set.of("application/json", "text/html", "application/octet-stream"));
        allowedExtensions.put(MediaType.VIRTUAL_TOUR, Set.of("json", "html", "htm"));
        maxFileSizes.put(MediaType.VIRTUAL_TOUR, 50 * 1024 * 1024L); // 50MB

        // Others
        allowedMimeTypes.put(MediaType.PANORAMA, Set.of("image/jpeg", "image/png", "image/webp"));
        allowedExtensions.put(MediaType.PANORAMA, Set.of("jpg", "jpeg", "png", "webp"));
        maxFileSizes.put(MediaType.PANORAMA, 20 * 1024 * 1024L);

        allowedMimeTypes.put(MediaType.GLB, Set.of("model/gltf-binary", "application/octet-stream"));
        allowedExtensions.put(MediaType.GLB, Set.of("glb"));
        maxFileSizes.put(MediaType.GLB, 100 * 1024 * 1024L);

        allowedMimeTypes.put(MediaType.GLTF, Set.of("model/gltf+json", "application/json"));
        allowedExtensions.put(MediaType.GLTF, Set.of("gltf"));
        maxFileSizes.put(MediaType.GLTF, 100 * 1024 * 1024L);

        allowedMimeTypes.put(MediaType.USDZ, Set.of("model/vnd.usdz+zip", "application/octet-stream"));
        allowedExtensions.put(MediaType.USDZ, Set.of("usdz"));
        maxFileSizes.put(MediaType.USDZ, 100 * 1024 * 1024L);

        allowedMimeTypes.put(MediaType.OBJ, Set.of("text/plain", "application/octet-stream"));
        allowedExtensions.put(MediaType.OBJ, Set.of("obj"));
        maxFileSizes.put(MediaType.OBJ, 100 * 1024 * 1024L);
    }

    @Override
    public int getMaxImagesPerProperty() {
        return 20;
    }

    @Override
    public int getMaxVideosPerProperty() {
        return 2;
    }

    @Override
    public int getMaxCoverImagesPerProperty() {
        return 1;
    }

    @Override
    public int getMaxVirtualToursPerProperty() {
        return 1;
    }

    @Override
    public int getMaxFloorPlansPerProperty() {
        return 1;
    }

    @Override
    public long getMaxFileSize(MediaType mediaType) {
        return maxFileSizes.getOrDefault(mediaType, 10 * 1024 * 1024L);
    }

    @Override
    public boolean isSupportedMimeType(MediaType mediaType, String mimeType) {
        if (mimeType == null) return false;
        Set<String> supported = allowedMimeTypes.get(mediaType);
        return supported != null && supported.contains(mimeType.toLowerCase());
    }

    @Override
    public boolean isSupportedExtension(MediaType mediaType, String extension) {
        if (extension == null) return false;
        Set<String> supported = allowedExtensions.get(mediaType);
        return supported != null && supported.contains(extension.toLowerCase());
    }
}
