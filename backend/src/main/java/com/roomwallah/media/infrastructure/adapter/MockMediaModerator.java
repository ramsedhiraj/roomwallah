package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.MediaModerationPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MockMediaModerator implements MediaModerationPort {

    @Override
    public boolean scanContent(byte[] content, String mimeType) {
        if (content == null) {
            return true;
        }
        String contentStr = new String(content, 0, Math.min(content.length, 100), StandardCharsets.UTF_8);
        if (contentStr.contains("NSFW") || contentStr.contains("BLOCKED")) {
            return false;
        }
        return true;
    }
}
