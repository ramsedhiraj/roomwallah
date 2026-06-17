package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.entity.UserSession;
import com.roomwallah.identity.domain.event.UserSessionCreatedEvent;
import com.roomwallah.identity.domain.event.UserSessionRevokedEvent;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String hash = refreshTokenService.hashToken(rawRefreshToken);
        Optional<UserSession> sessionOpt = sessionService.getSessionByHash(hash);

        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UserSession oldSession = sessionOpt.get();
        if (oldSession.isRevoked()) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (oldSession.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = oldSession.getUser();

        sessionService.revokeSession(oldSession);

        // Publish session revocation event
        UserSessionRevokedEvent revokedEvent = UserSessionRevokedEvent.builder()
                .sessionId(oldSession.getId())
                .userId(user.getId())
                .revokedAt(Instant.now(clock))
                .build();
        eventPublisher.publish(revokedEvent);

        String newRawRefreshToken = refreshTokenService.generateRawToken();
        String newHashedRefreshToken = refreshTokenService.hashToken(newRawRefreshToken);

        Instant expiresAt = Instant.now(clock).plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        UserSession newSession = sessionService.createSession(
                user,
                newHashedRefreshToken,
                oldSession.getDeviceName(),
                oldSession.getBrowser(),
                oldSession.getOperatingSystem(),
                oldSession.getIpAddress(),
                expiresAt
        );

        // Publish session creation event
        UserSessionCreatedEvent createdEvent = UserSessionCreatedEvent.builder()
                .sessionId(newSession.getId())
                .userId(user.getId())
                .ipAddress(newSession.getIpAddress())
                .createdAt(Instant.now(clock))
                .build();
        eventPublisher.publish(createdEvent);

        String newAccessToken = accessTokenService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }
}
