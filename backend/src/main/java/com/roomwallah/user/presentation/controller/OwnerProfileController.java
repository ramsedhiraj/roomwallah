package com.roomwallah.user.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.user.presentation.dto.PublicOwnerProfileResponse;
import com.roomwallah.user.service.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
@Tag(name = "Owner Public Profile", description = "Public read-only profiles for property owners")
public class OwnerProfileController {

    private final UserFacade userFacade;

    @GetMapping("/{id}/public-profile")
    @Operation(summary = "Get read-only public owner profile by userId")
    public ApiResponse<PublicOwnerProfileResponse> getPublicProfile(@PathVariable("id") UUID ownerId) {
        log.info("Request received to fetch public owner profile for ownerId: {}", ownerId);
        PublicOwnerProfileResponse publicProfile = userFacade.getPublicOwnerProfile(ownerId);
        return ApiResponse.success(publicProfile, "Public owner profile retrieved successfully");
    }
}
