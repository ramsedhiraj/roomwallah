package com.roomwallah.user.service;

import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.presentation.dto.AvatarUploadResponse;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;
import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final CurrentUserProvider currentUserProvider;
    private final UserProfileService userProfileService;
    private final PasswordManagementService passwordManagementService;
    private final AccountLifecycleService accountLifecycleService;
    private final AvatarService avatarService;
    private final OwnerProfileService ownerProfileService;

    @Override
    public UserProfileResponse getProfile() {
        User user = currentUserProvider.getCurrentUser();
        return userProfileService.getProfile(user);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return userProfileService.updateProfile(user, request);
    }

    @Override
    public AvatarUploadResponse uploadAvatar(InputStream inputStream, String originalFileName, String contentType) {
        User user = currentUserProvider.getCurrentUser();
        return avatarService.uploadAvatar(user, inputStream, originalFileName, contentType);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();
        passwordManagementService.changePassword(user, request);
    }

    @Override
    public void deactivateAccount() {
        User user = currentUserProvider.getCurrentUser();
        accountLifecycleService.deactivateAccount(user);
    }

    @Override
    public PublicOwnerProfileResponse getPublicOwnerProfile(UUID ownerId) {
        return ownerProfileService.getPublicOwnerProfile(ownerId);
    }
}
