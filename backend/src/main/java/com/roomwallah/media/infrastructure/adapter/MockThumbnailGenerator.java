package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.ThumbnailGeneratorPort;
import org.springframework.stereotype.Component;

@Component
public class MockThumbnailGenerator implements ThumbnailGeneratorPort {

    @Override
    public byte[] generateThumbnail(byte[] content, String mimeType) {
        // Just return the original content or a mock thumbnail
        return content;
    }
}
