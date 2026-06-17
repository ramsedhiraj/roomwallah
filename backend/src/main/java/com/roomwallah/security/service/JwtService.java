package com.roomwallah.security.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JwtService {

    public String extractUsername(String token) {
        // Placeholder implementation
        return null;
    }

    public String generateToken(String username) {
        // Placeholder implementation
        return "placeholder-jwt-token-for-" + username;
    }

    public String generateToken(Map<String, Object> extraClaims, String username) {
        // Placeholder implementation
        return "placeholder-jwt-token-for-" + username;
    }

    public boolean isTokenValid(String token, String username) {
        // Placeholder implementation
        return true;
    }
}
