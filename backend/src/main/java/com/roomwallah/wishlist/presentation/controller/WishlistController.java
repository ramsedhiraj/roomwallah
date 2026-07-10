package com.roomwallah.wishlist.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.search.presentation.dto.PropertyCardDto;
import com.roomwallah.wishlist.application.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Endpoints for managing user wishlist/saved properties")
public class WishlistController {

    private final WishlistService wishlistService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{propertyId}")
    @Operation(summary = "Add a property to the user's wishlist")
    public ApiResponse<Void> addToWishlist(@PathVariable("propertyId") UUID propertyId) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        log.info("REST request to add property {} to wishlist for user {}", propertyId, userId);
        wishlistService.addToWishlist(userId, propertyId);
        return ApiResponse.success(null, "Property added to wishlist");
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Remove a property from the user's wishlist")
    public ApiResponse<Void> removeFromWishlist(@PathVariable("propertyId") UUID propertyId) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        log.info("REST request to remove property {} from wishlist for user {}", propertyId, userId);
        wishlistService.removeFromWishlist(userId, propertyId);
        return ApiResponse.success(null, "Property removed from wishlist");
    }

    @GetMapping
    @Operation(summary = "Get the user's wishlist")
    public ApiResponse<List<PropertyCardDto>> getWishlist() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        log.info("REST request to get wishlist for user {}", userId);
        List<PropertyCardDto> wishlist = wishlistService.getWishlist(userId);
        return ApiResponse.success(wishlist, "Wishlist retrieved successfully");
    }

    @GetMapping("/check")
    @Operation(summary = "Batch check if properties are in the user's wishlist")
    public ApiResponse<Set<UUID>> batchCheckWishlist(@RequestParam("propertyIds") List<String> propertyIds) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        log.info("REST request to batch check wishlist for user {}", userId);
        List<UUID> uuids = propertyIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
        Set<UUID> wishlisted = wishlistService.batchCheckWishlist(userId, uuids);
        return ApiResponse.success(wishlisted, "Batch check completed successfully");
    }
}
