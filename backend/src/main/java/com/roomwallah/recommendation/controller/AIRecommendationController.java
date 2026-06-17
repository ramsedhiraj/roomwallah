package com.roomwallah.recommendation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.recommendation.domain.RecommendationWeight;
import com.roomwallah.recommendation.dto.RecommendationResponse;
import com.roomwallah.recommendation.service.RecommendationService;
import com.roomwallah.recommendation.service.VectorRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/recommendations", "/api/v1/ai/recommendations"})
@RequiredArgsConstructor
@Tag(name = "Recommendation API", description = "AI recommendations and personalization endpoints")
public class AIRecommendationController {

    private final RecommendationService recommendationService;
    private final VectorRecommendationService vectorRecommendationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/interactions")
    public ResponseEntity<Void> logInteraction(
            @RequestParam(required = false) UUID userId,
            @RequestParam UUID listingId,
            @RequestParam(defaultValue = "VIEW") String type
    ) {
        recommendationService.logInteraction(userId, listingId, type);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/similar/{listingId}")
    public ResponseEntity<List<RecommendationResponse>> getSimilarListings(@PathVariable UUID listingId) {
        List<RecommendationResponse> responses = recommendationService.getSimilarListingsCached(listingId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Property>> getTrendingListings(@RequestParam(defaultValue = "5") int limit) {
        List<Property> trending = recommendationService.getTrendingListingsCached(limit);
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/weights")
    public ResponseEntity<RecommendationWeight> getWeights() {
        RecommendationWeight weights = recommendationService.getWeights();
        return ResponseEntity.ok(weights);
    }

    @PutMapping("/weights")
    public ResponseEntity<RecommendationWeight> updateWeights(
            @RequestParam double budget,
            @RequestParam double proximity,
            @RequestParam double recency,
            @RequestParam double popularity
    ) {
        RecommendationWeight updated = recommendationService.updateWeights(budget, proximity, recency, popularity);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/ctr")
    @Operation(summary = "Track user clicks on personalized recommendations")
    public ApiResponse<String> trackCtrClick(@RequestBody Map<String, String> body) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        UUID propertyId = UUID.fromString(body.get("propertyId"));
        String action = body.get("action");
        vectorRecommendationService.trackClick(userId, propertyId, action);
        return ApiResponse.success("CTR Click logged successfully");
    }

    @PostMapping("/refine")
    @Operation(summary = "Refine user vector recommendations preference parameters")
    public ApiResponse<String> refinePreferences(@RequestBody Map<String, Object> body) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        double budgetLimit = ((Number) body.get("budgetLimit")).doubleValue();
        String targetCity = (String) body.get("targetCity");
        boolean hasGym = (Boolean) body.get("hasGym");
        boolean hasParking = (Boolean) body.get("hasParking");

        vectorRecommendationService.refinePreferences(userId, budgetLimit, targetCity, hasGym, hasParking);
        return ApiResponse.success("Vector preferences refined successfully");
    }

    @GetMapping("/personalized-feed")
    @Operation(summary = "Get personalized feed based on user vector preferences")
    public ApiResponse<List<Property>> getPersonalizedFeed() {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        List<Property> feed = vectorRecommendationService.getPersonalizedFeed(userId);
        return ApiResponse.success(feed, "Personalized feed generated successfully");
    }
}
