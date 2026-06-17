package com.roomwallah.user.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AvatarUpdatedEvent {
    private final UUID userId;
    private final String avatarKey;
    private final Instant updatedAt;
}
