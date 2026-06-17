package com.roomwallah.identity.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserSessionRevokedEvent {
    private final UUID sessionId;
    private final UUID userId;
    private final Instant revokedAt;
}
