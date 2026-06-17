package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.entity.UserSession;
import com.roomwallah.identity.domain.event.UserLoggedOutEvent;
import com.roomwallah.identity.domain.event.UserSessionRevokedEvent;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutService {

    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String hash = refreshTokenService.hashToken(rawRefreshToken);
        Optional<UserSession> sessionOpt = sessionService.getSessionByHash(hash);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            if (!session.isRevoked()) {
                sessionService.revokeSession(session);

                UserLoggedOutEvent event = UserLoggedOutEvent.builder()
                        .userId(session.getUser().getId())
                        .sessionId(session.getId())
                        .loggedOutAt(Instant.now(clock))
                        .build();
                eventPublisher.publish(event);

                UserSessionRevokedEvent revokedEvent = UserSessionRevokedEvent.builder()
                        .sessionId(session.getId())
                        .userId(session.getUser().getId())
                        .revokedAt(Instant.now(clock))
                        .build();
                eventPublisher.publish(revokedEvent);

                log.info("Session revoked successfully for logout.");
            }
        } else {
            log.warn("Logout request with invalid or missing session token.");
        }
    }
}
