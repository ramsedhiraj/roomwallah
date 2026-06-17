package com.roomwallah.media.domain.port;

public interface ThumbnailGeneratorPort {
    byte[] generateThumbnail(byte[] content, String mimeType);
}
