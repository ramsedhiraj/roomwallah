package com.roomwallah.media.domain.port;

public interface ImageOptimizerPort {
    byte[] optimize(byte[] content, String mimeType);
}
