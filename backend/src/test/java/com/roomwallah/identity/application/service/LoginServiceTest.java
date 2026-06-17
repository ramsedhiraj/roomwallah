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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationProviderFactory authenticationProviderFactory;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private SessionService sessionService;
    @Mock
    private RateLimiterPort rateLimiterPort;
    @Mock
    private EventPublisherPort eventPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private AuthenticationProviderStrategy authenticationProviderStrategy;

    private Clock clock;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneId.of("UTC"));
        loginService = new LoginServiceImpl(
                authenticationProviderFactory,
                accessTokenService,
                refreshTokenService,
                sessionService,
                rateLimiterPort,
                eventPublisher,
                clock,
                userRepository,
                transactionManager
        );
    }

    @Test
    void login_whenRateLimited_throwsException() {
        // Arrange
        String identity = "bob@example.com";
        when(rateLimiterPort.isBlocked(identity)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                loginService.login(identity, "Password123!", AuthType.PASSWORD, "Device", "Chrome", "Windows", "127.0.0.1")
        );
        verify(rateLimiterPort, never()).recordFailedAttempt(anyString());
        verifyNoInteractions(authenticationProviderFactory);
    }

    @Test
    void login_withValidCredentials_authenticatesCreatesSessionAndPublishesEvents() {
        // Arrange
        String identity = "bob@example.com";
        String password = "Password123!";
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(identity);
        mockUser.setRole(com.roomwallah.user.entity.UserRole.TENANT);

        UserSession mockSession = new UserSession();
        mockSession.setId(sessionId);
        mockSession.setUser(mockUser);

        when(rateLimiterPort.isBlocked(identity)).thenReturn(false);
        when(authenticationProviderFactory.getStrategy(AuthType.PASSWORD)).thenReturn(authenticationProviderStrategy);
        when(authenticationProviderStrategy.authenticate(identity, password)).thenReturn(mockUser);
        
        when(accessTokenService.generateToken(mockUser)).thenReturn("access_token");
        when(refreshTokenService.generateRawToken()).thenReturn("raw_refresh_token");
        when(refreshTokenService.hashToken("raw_refresh_token")).thenReturn("hashed_refresh_token");
        
        when(sessionService.createSession(eq(mockUser), eq("hashed_refresh_token"), anyString(), anyString(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(mockSession);

        // Act
        AuthResponse response = loginService.login(identity, password, AuthType.PASSWORD, "Device", "Chrome", "Windows", "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("raw_refresh_token", response.getRefreshToken());
        assertEquals("TENANT", response.getRole());

        verify(rateLimiterPort).resetAttempts(identity);
        verify(userRepository).save(mockUser);
        verify(eventPublisher).publish(any(UserLoggedInEvent.class));
        verify(eventPublisher).publish(any(UserSessionCreatedEvent.class));
    }
}
