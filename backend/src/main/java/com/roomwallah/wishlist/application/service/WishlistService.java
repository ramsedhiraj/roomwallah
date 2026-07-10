package com.roomwallah.wishlist.application.service;

import com.roomwallah.search.presentation.dto.PropertyCardDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WishlistService {
    void addToWishlist(UUID userId, UUID propertyId);
    void removeFromWishlist(UUID userId, UUID propertyId);
    List<PropertyCardDto> getWishlist(UUID userId);
    boolean isInWishlist(UUID userId, UUID propertyId);
    Set<UUID> batchCheckWishlist(UUID userId, List<UUID> propertyIds);
}
