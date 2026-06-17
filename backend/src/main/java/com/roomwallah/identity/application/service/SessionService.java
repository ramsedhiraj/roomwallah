package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.entity.UserSession;
import com.roomwallah.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionService {
    UserSession createSession(User user, String tokenHash, String deviceName, String browser, String os, String ipAddress, Instant expiresAt);
    Optional<UserSession> getSessionByHash(String tokenHash);
    void revokeSession(UserSession session);
    void revokeAllUserSessions(UUID userId);
    List<UserSession> getActiveSessions(UUID userId);
}
