package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.ImageOptimizerPort;
import org.springframework.stereotype.Component;

@Component
public class MockImageOptimizer implements ImageOptimizerPort {

    @Override
    public byte[] optimize(byte[] content, String mimeType) {
        // Just return the original content in mock mode
        return content;
    }
}
