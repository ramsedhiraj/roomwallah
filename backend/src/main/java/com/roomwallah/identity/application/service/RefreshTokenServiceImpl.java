package com.roomwallah.identity.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateRawToken() {
        byte[] values = new byte[32];
        secureRandom.nextBytes(values);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(values);
    }

    @Override
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to hash refresh token", e);
            throw new IllegalStateException("Could not hash token", e);
        }
    }
}
