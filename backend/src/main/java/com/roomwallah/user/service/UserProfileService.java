package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getProfile(User user);
    UserProfileResponse updateProfile(User user, UpdateProfileRequest request);
}
