package com.roomwallah.user.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.user.presentation.dto.AvatarUploadResponse;
import com.roomwallah.user.presentation.dto.ChangePasswordRequest;
import com.roomwallah.user.presentation.dto.UpdateProfileRequest;
import com.roomwallah.user.presentation.dto.UserProfileResponse;
import com.roomwallah.user.service.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts, profiles, and preferences")
public class UserController {

    private final UserFacade userFacade;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile and preferences")
    public ApiResponse<UserProfileResponse> getProfile() {
        log.info("Request received to fetch profile for current authenticated user");
        UserProfileResponse profile = userFacade.getProfile();
        return ApiResponse.success(profile, "User profile retrieved successfully");
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user's profile and preferences")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        log.info("Request received to update profile details");
        UserProfileResponse updatedProfile = userFacade.updateProfile(request);
        return ApiResponse.success(updatedProfile, "User profile updated successfully");
    }

    @PostMapping("/me/avatar")
    @Operation(summary = "Upload/update avatar photo for the current user")
    public ApiResponse<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        log.info("Request received to upload avatar: name={}, size={}", file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be empty");
        }

        try {
            AvatarUploadResponse response = userFacade.uploadAvatar(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
            return ApiResponse.success(response, "Avatar uploaded successfully");
        } catch (IOException e) {
            log.error("Failed to read uploaded avatar file", e);
            throw new IllegalArgumentException("Invalid file upload");
        }
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change current authenticated user's password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Request received to update password");
        userFacade.changePassword(request);
        return ApiResponse.success(null, "Password changed successfully");
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate/soft-delete the current user account")
    public ApiResponse<Void> deactivateAccount() {
        log.info("Request received to deactivate current user account");
        userFacade.deactivateAccount();
        return ApiResponse.success(null, "Account deactivated successfully");
    }
}
