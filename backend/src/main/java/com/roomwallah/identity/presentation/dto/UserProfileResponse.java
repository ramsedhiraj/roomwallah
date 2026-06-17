package com.roomwallah.identity.presentation.dto;

import com.roomwallah.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {
    private final UUID id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;
}
