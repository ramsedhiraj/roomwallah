package com.roomwallah.identity.domain.port;

import java.io.InputStream;

public interface ObjectStoragePort {
    void uploadFile(InputStream inputStream, String objectKey, String contentType);
    void deleteFile(String objectKey);
    String getPublicUrl(String objectKey);
}
