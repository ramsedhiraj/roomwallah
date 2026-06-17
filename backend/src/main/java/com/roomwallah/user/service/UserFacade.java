package com.roomwallah.user.service;

import com.roomwallah.user.presentation.dto.AvatarUploadResponse;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;
import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;

import java.io.InputStream;
import java.util.UUID;

public interface UserFacade {
    UserProfileResponse getProfile();
    UserProfileResponse updateProfile(UpdateProfileRequest request);
    AvatarUploadResponse uploadAvatar(InputStream inputStream, String originalFileName, String contentType);
    void changePassword(ChangePasswordRequest request);
    void deactivateAccount();
    PublicOwnerProfileResponse getPublicOwnerProfile(UUID ownerId);
}
