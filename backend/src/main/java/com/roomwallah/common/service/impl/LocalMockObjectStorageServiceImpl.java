package com.roomwallah.common.service.impl;

import com.roomwallah.common.service.ObjectStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class LocalMockObjectStorageServiceImpl implements ObjectStorageService {

    @Override
    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        log.info("Mocking file upload for file: {}, contentType: {}", fileName, contentType);
        return "https://storage.googleapis.com/roomwallah-mock-bucket/" + fileName;
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("Mocking file deletion for URL: {}", fileUrl);
    }
}
