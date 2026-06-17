package com.roomwallah.identity.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserLoggedInEvent {
    private final UUID userId;
    private final UUID sessionId;
    private final String email;
    private final String ipAddress;
    private final String deviceName;
    private final Instant loggedInAt;
}
