package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.VirusScannerPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MockVirusScanner implements VirusScannerPort {

    @Override
    public boolean scan(byte[] content) {
        if (content == null) {
            return true;
        }
        String contentStr = new String(content, 0, Math.min(content.length, 100), StandardCharsets.UTF_8);
        // Simulate finding a virus for test cases
        if (contentStr.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE") || contentStr.contains("INFECTED")) {
            return false;
        }
        return true;
    }
}
