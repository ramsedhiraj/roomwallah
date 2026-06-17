package com.roomwallah.media.domain.port;

public interface PerceptualHasherPort {
    String calculateHash(byte[] content);
    double calculateDistance(String hash1, String hash2);
}
