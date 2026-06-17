package com.roomwallah.user.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserProfileUpdatedEvent {
    private final UUID userId;
    private final String email;
    private final String fullName;
    private final Instant updatedAt;
}
