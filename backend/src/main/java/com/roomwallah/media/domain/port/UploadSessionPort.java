package com.roomwallah.media.domain.port;

import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.valueobject.UploadSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UploadSessionPort {
    UploadSession createSession(UUID propertyId, String filename, long totalSize, MediaType mediaType);
    UploadSession getSession(String sessionId);
    void uploadChunk(String sessionId, int chunkNumber, byte[] chunkData);
    byte[] assembleSession(String sessionId);
    void deleteSession(String sessionId);
    boolean exists(String sessionId);
    List<String> getExpiredSessionIds(Instant expirationTime);
}
