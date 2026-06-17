package com.roomwallah.media.infrastructure.adapter;

import com.roomwallah.media.domain.port.PerceptualHasherPort;
import org.springframework.stereotype.Component;

@Component
public class MockPerceptualHasher implements PerceptualHasherPort {

    @Override
    public String calculateHash(byte[] content) {
        if (content == null || content.length == 0) {
            return "0000000000000000";
        }
        long sum = 0;
        for (byte b : content) {
            sum += Math.abs(b);
        }
        return String.format("%016X", sum);
    }

    @Override
    public double calculateDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length()) {
            return 1.0;
        }
        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance++;
            }
        }
        return (double) distance / hash1.length();
    }
}
