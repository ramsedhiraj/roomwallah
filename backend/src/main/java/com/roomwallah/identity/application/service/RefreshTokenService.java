package com.roomwallah.identity.application.service;

public interface RefreshTokenService {
    String generateRawToken();
    String hashToken(String rawToken);
}
