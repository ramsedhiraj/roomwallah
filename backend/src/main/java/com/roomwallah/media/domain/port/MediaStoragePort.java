package com.roomwallah.media.domain.port;

import java.io.InputStream;

public interface MediaStoragePort {
    void store(String key, InputStream content, String mimeType, long contentLength);
    InputStream retrieve(String key);
    void delete(String key);
    String generateUrl(String key);
    String generatePresignedUploadUrl(String key, long expirationSeconds);
    String generatePresignedDownloadUrl(String key, long expirationSeconds);
}
