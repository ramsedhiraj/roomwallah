package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.ObjectStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class LocalMockObjectStorageAdapter implements ObjectStoragePort {

    @Override
    public void uploadFile(InputStream inputStream, String objectKey, String contentType) {
        log.info("Mock Object Storage - Uploading file to objectKey: {}, contentType: {}", objectKey, contentType);
    }

    @Override
    public void deleteFile(String objectKey) {
        log.info("Mock Object Storage - Deleting file with objectKey: {}", objectKey);
    }

    @Override
    public String getPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        // Return a mock public storage URL for testing
        return "https://storage.googleapis.com/roomwallah-mock-bucket/" + objectKey;
    }
}
