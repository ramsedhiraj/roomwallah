package com.roomwallah.media;

import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.infrastructure.adapter.DefaultMediaPolicyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MediaPolicyTest {

    private DefaultMediaPolicyAdapter mediaPolicyAdapter;

    @BeforeEach
    public void setUp() {
        mediaPolicyAdapter = new DefaultMediaPolicyAdapter();
    }

    @Test
    public void testPolicyLimits() {
        assertEquals(20, mediaPolicyAdapter.getMaxImagesPerProperty());
        assertEquals(2, mediaPolicyAdapter.getMaxVideosPerProperty());
        assertEquals(1, mediaPolicyAdapter.getMaxCoverImagesPerProperty());
        assertEquals(1, mediaPolicyAdapter.getMaxVirtualToursPerProperty());
        assertEquals(1, mediaPolicyAdapter.getMaxFloorPlansPerProperty());
    }

    @Test
    public void testSupportedMimeTypes() {
        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.IMAGE, "image/jpeg"));
        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.IMAGE, "image/png"));
        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.IMAGE, "image/webp"));
        assertFalse(mediaPolicyAdapter.isSupportedMimeType(MediaType.IMAGE, "application/pdf"));

        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.VIDEO, "video/mp4"));
        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.VIDEO, "video/webm"));
        assertFalse(mediaPolicyAdapter.isSupportedMimeType(MediaType.VIDEO, "image/jpeg"));

        assertTrue(mediaPolicyAdapter.isSupportedMimeType(MediaType.FLOOR_PLAN, "application/pdf"));
    }

    @Test
    public void testSupportedExtensions() {
        assertTrue(mediaPolicyAdapter.isSupportedExtension(MediaType.IMAGE, "jpg"));
        assertTrue(mediaPolicyAdapter.isSupportedExtension(MediaType.IMAGE, "png"));
        assertFalse(mediaPolicyAdapter.isSupportedExtension(MediaType.IMAGE, "pdf"));

        assertTrue(mediaPolicyAdapter.isSupportedExtension(MediaType.VIDEO, "mp4"));
        assertTrue(mediaPolicyAdapter.isSupportedExtension(MediaType.FLOOR_PLAN, "pdf"));
    }
}
