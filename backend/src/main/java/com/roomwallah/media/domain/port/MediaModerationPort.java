package com.roomwallah.media.domain.port;

public interface MediaModerationPort {
    boolean scanContent(byte[] content, String mimeType);
}
