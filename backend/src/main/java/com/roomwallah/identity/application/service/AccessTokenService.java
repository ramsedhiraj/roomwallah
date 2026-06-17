package com.roomwallah.identity.application.service;

import com.roomwallah.user.entity.User;

public interface AccessTokenService {
    String generateToken(User user);
    String extractUserId(String token);
    String extractRole(String token);
    boolean isTokenExpired(String token);
}
