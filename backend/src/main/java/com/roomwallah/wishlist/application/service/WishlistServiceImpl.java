package com.roomwallah.wishlist.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.search.presentation.dto.PropertyCardDto;
import com.roomwallah.wishlist.domain.entity.WishlistItem;
import com.roomwallah.wishlist.domain.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final PropertyRepository propertyRepository;
    private final SearchDocumentRepository searchDocumentRepository;

    @Override
    @Transactional
    public void addToWishlist(UUID userId, UUID propertyId) {
        log.info("Adding property {} to wishlist for user {}", propertyId, userId);
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + propertyId));
        
        if (property.isDeleted()) {
            throw new IllegalArgumentException("Cannot wishlist a deleted property");
        }

        if (wishlistRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            log.info("Property {} is already in user {}'s wishlist", propertyId, userId);
            return;
        }

        WishlistItem item = new WishlistItem();
        item.setUserId(userId);
        item.setPropertyId(propertyId);
        wishlistRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromWishlist(UUID userId, UUID propertyId) {
        log.info("Removing property {} from wishlist for user {}", propertyId, userId);
        wishlistRepository.deleteByUserIdAndPropertyId(userId, propertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyCardDto> getWishlist(UUID userId) {
        log.info("Fetching wishlist for user {}", userId);
        List<WishlistItem> items = wishlistRepository.findByUserId(userId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> propertyIds = items.stream()
                .map(WishlistItem::getPropertyId)
                .collect(Collectors.toList());

        List<SearchDocument> docs = searchDocumentRepository.findAllById(propertyIds);
        
        Map<UUID, SearchDocument> docMap = docs.stream()
                .collect(Collectors.toMap(SearchDocument::getPropertyId, doc -> doc));

        return propertyIds.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .map(this::toPropertyCard)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(UUID userId, UUID propertyId) {
        return wishlistRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> batchCheckWishlist(UUID userId, List<UUID> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<UUID> result = new HashSet<>();
        for (UUID pid : propertyIds) {
            if (wishlistRepository.existsByUserIdAndPropertyId(userId, pid)) {
                result.add(pid);
            }
        }
        return result;
    }

    private PropertyCardDto toPropertyCard(SearchDocument doc) {
        String thumbnailUrl = null;
        if (doc.getMediaCount() > 0) {
            thumbnailUrl = "/api/v1/media/properties/" + doc.getPropertyId() + "/thumbnail?width=400&height=300";
        }
        return PropertyCardDto.builder()
                .propertyId(doc.getPropertyId())
                .listingRef(doc.getListingRef())
                .slug(doc.getSlug())
                .title(doc.getTitle())
                .city(doc.getCity())
                .locality(doc.getLocality())
                .price(doc.getPrice())
                .propertyType(doc.getPropertyType())
                .listingPurpose(doc.getListingPurpose())
                .bedrooms(doc.getBedrooms())
                .bathrooms(doc.getBathrooms())
                .parkingCount(doc.getParkingCount())
                .furnishingStatus(doc.getFurnishingStatus())
                .petFriendly(doc.isPetFriendly())
                .trustScore(doc.getTrustScore())
                .ownerVerified(doc.isOwnerVerified())
                .ownerBadge(doc.getOwnerBadge())
                .mediaCount(doc.getMediaCount())
                .publishedAt(doc.getPublishedAt())
                .thumbnailUrl(thumbnailUrl)
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .rankingExplanation(doc.getRankingExplanation())
                .build();
    }
}
