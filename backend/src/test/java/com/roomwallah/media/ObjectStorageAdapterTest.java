package com.roomwallah.media;

import com.roomwallah.media.infrastructure.adapter.LocalStorageAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectStorageAdapterTest {

    private LocalStorageAdapter storageAdapter;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        storageAdapter = new LocalStorageAdapter(tempDir.toString());
        storageAdapter.init();
    }

    @Test
    public void testStoreAndRetrieve() throws IOException {
        String key = "test/file.txt";
        byte[] content = "Hello RoomWallah Storage".getBytes();

        storageAdapter.store(key, new ByteArrayInputStream(content), "text/plain", content.length);

        try (InputStream is = storageAdapter.retrieve(key)) {
            byte[] retrieved = is.readAllBytes();
            assertArrayEquals(content, retrieved);
        }
    }

    @Test
    public void testDelete() throws IOException {
        String key = "test/delete-file.txt";
        byte[] content = "Delete me".getBytes();

        storageAdapter.store(key, new ByteArrayInputStream(content), "text/plain", content.length);
        
        // Assert can retrieve
        try (InputStream is = storageAdapter.retrieve(key)) {
            assertNotNull(is);
        }

        // Delete
        storageAdapter.delete(key);

        // Assert retrieval throws exception
        assertThrows(RuntimeException.class, () -> storageAdapter.retrieve(key));
    }

    @Test
    public void testGenerateUrl() {
        String key = "properties/123/image.png";
        String url = storageAdapter.generateUrl(key);
        assertEquals("/api/v1/media/files/" + key, url);
    }
}
