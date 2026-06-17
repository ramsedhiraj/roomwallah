package com.roomwallah.identity.application.service;

import com.roomwallah.identity.domain.entity.UserSession;
import com.roomwallah.identity.domain.event.UserLoggedInEvent;
import com.roomwallah.identity.domain.event.UserSessionCreatedEvent;
import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.domain.port.AuthenticationProviderStrategy;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.identity.domain.port.RateLimiterPort;
import com.roomwallah.identity.infrastructure.adapter.AuthenticationProviderFactory;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final AuthenticationProviderFactory authenticationProviderFactory;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final RateLimiterPort rateLimiterPort;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    @Transactional
    public AuthResponse login(
            String identity,
            String credentials,
            AuthType authType,
            String deviceName,
            String browser,
            String os,
            String ipAddress
    ) {
        // 1. Rate limiter check
        if (rateLimiterPort.isBlocked(identity)) {
            throw new IllegalArgumentException("Too many failed login attempts. Please try again later.");
        }

        try {
            // 2. Authenticate
            AuthenticationProviderStrategy strategy = authenticationProviderFactory.getStrategy(authType);
            User user = strategy.authenticate(identity, credentials);

            // Update user security metadata on successful login
            user.setFailedLoginCount(0);
            user.setLastSuccessfulLoginAt(Instant.now(clock));
            userRepository.save(user);

            // 3. Reset rate limiter attempts
            rateLimiterPort.resetAttempts(identity);

            // 4. Generate access token
            String accessToken = accessTokenService.generateToken(user);

            // 5. Generate refresh token
            String rawRefreshToken = refreshTokenService.generateRawToken();
            String hashedRefreshToken = refreshTokenService.hashToken(rawRefreshToken);

            // 6. Save session
            Instant expiresAt = Instant.now(clock).plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
            UserSession session = sessionService.createSession(
                    user,
                    hashedRefreshToken,
                    deviceName,
                    browser,
                    os,
                    ipAddress,
                    expiresAt
            );

            // 7. Publish Events
            UserLoggedInEvent loggedInEvent = UserLoggedInEvent.builder()
                    .userId(user.getId())
                    .sessionId(session.getId())
                    .email(user.getEmail())
                    .ipAddress(ipAddress)
                    .deviceName(deviceName)
                    .loggedInAt(Instant.now(clock))
                    .build();
            eventPublisher.publish(loggedInEvent);

            UserSessionCreatedEvent sessionCreatedEvent = UserSessionCreatedEvent.builder()
                    .sessionId(session.getId())
                    .userId(user.getId())
                    .ipAddress(ipAddress)
                    .createdAt(Instant.now(clock))
                    .build();
            eventPublisher.publish(sessionCreatedEvent);

            // 8. Build DTO Response
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(rawRefreshToken)
                    .tokenType("Bearer")
                    .role(user.getRole().name())
                    .email(user.getEmail())
                    .build();

        } catch (Exception e) {
            // Record failed attempt in rate limiter
            rateLimiterPort.recordFailedAttempt(identity);

            // Record failed attempt on User aggregate if it is a password mismatch (user exists)
            if (e instanceof IllegalArgumentException && "Invalid email/phone or password".equals(e.getMessage())) {
                recordFailedLoginAttempt(identity);
            }

            throw e;
        }
    }

    private void recordFailedLoginAttempt(String identity) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(status -> {
                Optional<User> userOpt = userRepository.findByEmail(identity);
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByPhone(identity);
                }
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setFailedLoginCount(user.getFailedLoginCount() + 1);
                    user.setLastFailedLoginAt(Instant.now(clock));
                    userRepository.save(user);
                }
            });
        } catch (Exception ex) {
            log.error("Error updating failed login stats for identity: {}", identity, ex);
        }
    }
}
