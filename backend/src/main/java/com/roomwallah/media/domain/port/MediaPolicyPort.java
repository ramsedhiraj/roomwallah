package com.roomwallah.media.domain.port;

import com.roomwallah.media.domain.entity.MediaType;

public interface MediaPolicyPort {
    int getMaxImagesPerProperty();
    int getMaxVideosPerProperty();
    int getMaxCoverImagesPerProperty();
    int getMaxVirtualToursPerProperty();
    int getMaxFloorPlansPerProperty();
    long getMaxFileSize(MediaType mediaType);
    boolean isSupportedMimeType(MediaType mediaType, String mimeType);
    boolean isSupportedExtension(MediaType mediaType, String extension);
}
