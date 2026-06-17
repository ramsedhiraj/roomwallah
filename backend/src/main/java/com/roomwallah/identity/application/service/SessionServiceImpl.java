package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.entity.UserSession;
import com.roomwallah.identity.infrastructure.repository.UserSessionRepository;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository sessionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public UserSession createSession(User user, String tokenHash, String deviceName, String browser, String os, String ipAddress, Instant expiresAt) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(tokenHash);
        session.setDeviceName(deviceName);
        session.setBrowser(browser);
        session.setOperatingSystem(os);
        session.setIpAddress(ipAddress);
        session.setLastUsedAt(Instant.now(clock));
        session.setExpiresAt(expiresAt);
        session.setRevoked(false);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSession> getSessionByHash(String tokenHash) {
        return sessionRepository.findByRefreshTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public void revokeSession(UserSession session) {
        session.setRevoked(true);
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        sessionRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> getActiveSessions(UUID userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }
}
