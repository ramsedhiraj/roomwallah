package com.roomwallah.user.service;

import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;

import java.util.UUID;

public interface OwnerProfileService {
    PublicOwnerProfileResponse getPublicOwnerProfile(UUID ownerId);
}
