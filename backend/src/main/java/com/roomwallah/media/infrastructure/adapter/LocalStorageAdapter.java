package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.MediaStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@Component
public class LocalStorageAdapter implements MediaStoragePort {

    private final Path rootPath;

    @Value("${roomwallah.media.cdn-prefix:}")
    private String cdnPrefix;

    public LocalStorageAdapter(@Value("${storage.location:storage}") String storagePath) {
        this.rootPath = Paths.get(storagePath);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + rootPath, e);
        }
    }

    @Override
    public void store(String key, InputStream content, String mimeType, long contentLength) {
        try {
            Path target = rootPath.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + key, e);
        }
    }

    @Override
    public InputStream retrieve(String key) {
        try {
            Path target = rootPath.resolve(key);
            if (!Files.exists(target)) {
                throw new RuntimeException("File not found: " + key);
            }
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path target = rootPath.resolve(key);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + key, e);
        }
    }

    @Override
    public String generateUrl(String key) {
        if (cdnPrefix != null && !cdnPrefix.isBlank()) {
            return cdnPrefix + "/" + key;
        }
        return "/api/v1/media/files/" + key;
    }

    @Override
    public String generatePresignedUploadUrl(String key, long expirationSeconds) {
        long expiresAt = Instant.now().getEpochSecond() + expirationSeconds;
        return generateUrl(key) + "?action=upload&expires=" + expiresAt + "&sig=mock_upload_sig";
    }

    @Override
    public String generatePresignedDownloadUrl(String key, long expirationSeconds) {
        long expiresAt = Instant.now().getEpochSecond() + expirationSeconds;
        return generateUrl(key) + "?action=download&expires=" + expiresAt + "&sig=mock_download_sig";
    }
}
