package com.roomwallah.identity.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserSessionCreatedEvent {
    private final UUID sessionId;
    private final UUID userId;
    private final String ipAddress;
    private final Instant createdAt;
}
