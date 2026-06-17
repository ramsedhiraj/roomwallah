package com.roomwallah.user.event;

import com.roomwallah.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserRegisteredEvent {
    private final UUID userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final Instant registeredAt;
}
