package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.port.UploadSessionPort;
import com.roomwallah.media.domain.valueobject.UploadSession;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryUploadSessionAdapter implements UploadSessionPort {

    private final Map<String, SessionContainer> sessions = new ConcurrentHashMap<>();

    private static class SessionContainer {
        final UploadSession session;
        final Instant createdAt;
        final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();

        SessionContainer(UploadSession session, Instant createdAt) {
            this.session = session;
            this.createdAt = createdAt;
        }
    }

    @Override
    public UploadSession createSession(UUID propertyId, String filename, long totalSize, MediaType mediaType) {
        String sessionId = UUID.randomUUID().toString();
        UploadSession session = new UploadSession(sessionId, propertyId, filename, totalSize, mediaType);
        sessions.put(sessionId, new SessionContainer(session, Instant.now()));
        return session;
    }

    @Override
    public UploadSession getSession(String sessionId) {
        SessionContainer container = sessions.get(sessionId);
        return container != null ? container.session : null;
    }

    @Override
    public void uploadChunk(String sessionId, int chunkNumber, byte[] chunkData) {
        SessionContainer container = sessions.get(sessionId);
        if (container == null) {
            throw new IllegalArgumentException("Upload session not found: " + sessionId);
        }
        container.chunks.put(chunkNumber, chunkData);
    }

    @Override
    public byte[] assembleSession(String sessionId) {
        SessionContainer container = sessions.get(sessionId);
        if (container == null) {
            throw new IllegalArgumentException("Upload session not found: " + sessionId);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TreeMap<Integer, byte[]> sortedChunks = new TreeMap<>(container.chunks);
            for (byte[] chunk : sortedChunks.values()) {
                outputStream.write(chunk);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to assemble upload chunks for session " + sessionId, e);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public List<String> getExpiredSessionIds(Instant expirationTime) {
        List<String> expired = new ArrayList<>();
        sessions.forEach((id, container) -> {
            if (container.createdAt.isBefore(expirationTime)) {
                expired.add(id);
            }
        });
        return expired;
    }
}
