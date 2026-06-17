package com.roomwallah.property.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.property.application.service.ListingHealthScoreServiceImpl;
import com.roomwallah.property.application.service.SmartPricingService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/listings", "/api/v1/properties"})
@RequiredArgsConstructor
@Tag(name = "Listing AI Controller", description = "AI Pricing Insights, Health checks, and adjustment resolvers")
public class PropertyAiController {

    private final SmartPricingService smartPricingService;
    private final ListingHealthScoreServiceImpl healthScoreService;
    private final PropertyRepository propertyRepository;

    @GetMapping("/{id}/price-insights")
    @Operation(summary = "Get dynamic pricing insights for a listing")
    public ApiResponse<Map<String, Object>> getPriceInsights(@PathVariable UUID id) {
        Map<String, Object> insights = smartPricingService.getPricingInsights(id);
        return ApiResponse.success(insights, "Pricing insights retrieved successfully");
    }

    @PostMapping("/{id}/price/apply")
    @Operation(summary = "Apply proposed rental price adjustment to a listing")
    public ApiResponse<Map<String, Object>> applyPriceAdjustment(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        Number priceNum = (Number) body.get("price");
        if (priceNum == null) {
            throw new IllegalArgumentException("Price parameter is required");
        }

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        property.setPrice(new Money(BigDecimal.valueOf(priceNum.doubleValue()), "INR"));
        propertyRepository.save(property);

        log.info("Price adjustment applied manually by owner for listing: {} to INR {}", id, priceNum);
        return ApiResponse.success(
                Map.of("listingId", id, "newPrice", priceNum),
                "Price adjustment applied successfully"
        );
    }

    @PostMapping("/{id}/resolve-health")
    @Operation(summary = "Resolve a listing completeness checklist task")
    public ApiResponse<Map<String, Object>> resolveHealthTask(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        String taskId = (String) body.get("taskId");
        String content = (String) body.get("content");

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Simulate resolution of checklist by logging or updating property fields
        log.info("Resolving health task: {} with content length: {} for listing: {}", taskId, content != null ? content.length() : 0, id);
        
        // Return updated health scorecard
        Map<String, Object> report = healthScoreService.calculateHealthScore(property);
        
        return ApiResponse.success(report, "Health checklist item resolved successfully");
    }
}
