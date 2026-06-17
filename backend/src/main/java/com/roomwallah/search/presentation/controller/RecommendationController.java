package com.roomwallah.search.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.search.application.facade.SearchFacade;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.port.RecommendationEnginePort;
import com.roomwallah.search.presentation.dto.PropertyCardDto;
import com.roomwallah.search.presentation.dto.RecommendationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized property recommendations")
public class RecommendationController {

    private final SearchFacade searchFacade;
    private final CurrentUserProvider currentUserProvider;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @GetMapping
    @Operation(summary = "Get personalized property recommendations for the authenticated user")
    public ApiResponse<List<RecommendationResponseDto>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit
    ) {
        String correlationId = MDC.get("correlationId");
        UUID userId = currentUserProvider.getCurrentUser().getId();
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        log.info("Recommendations request - userId: {}, limit: {}, correlationId: {}", userId, clampedLimit, correlationId);

        List<RecommendationEnginePort.RecommendationItem> items = searchFacade.getRecommendations(userId, clampedLimit);

        List<RecommendationResponseDto> response = items.stream()
                .map(item -> RecommendationResponseDto.builder()
                        .property(toPropertyCard(item.document()))
                        .reasons(item.reasons())
                        .build())
                .collect(Collectors.toList());

        log.info("Recommendations returned - userId: {}, count: {}, correlationId: {}", userId, response.size(), correlationId);

        return ApiResponse.success(response, "Recommendations retrieved successfully");
    }

    private PropertyCardDto toPropertyCard(SearchDocument doc) {
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
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .build();
    }
}
