package com.roomwallah.identity.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserLoggedOutEvent {
    private final UUID userId;
    private final UUID sessionId;
    private final Instant loggedOutAt;
}
