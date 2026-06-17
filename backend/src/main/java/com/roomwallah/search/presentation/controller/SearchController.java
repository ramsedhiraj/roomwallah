package com.roomwallah.search.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.exception.RateLimitExceededException;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.search.application.facade.SearchFacade;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.entity.TrendingQuery;
import com.roomwallah.search.domain.model.CursorPage;
import com.roomwallah.search.domain.model.GeoRadius;
import com.roomwallah.search.domain.model.PriceRange;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.model.SortOption;
import com.roomwallah.search.domain.port.SearchEnginePort;
import com.roomwallah.search.presentation.dto.AutoCompleteResponseDto;
import com.roomwallah.search.presentation.dto.PropertyCardDto;
import com.roomwallah.search.presentation.dto.SearchRequestDto;
import com.roomwallah.search.presentation.dto.SearchResponseDto;
import com.roomwallah.search.presentation.dto.TrendingQueryDto;
import com.roomwallah.security.limiter.RedisRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.roomwallah.search.application.service.SearchExperimentRouter;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Property search, autocomplete, and trending queries")
public class SearchController {

    private final SearchFacade searchFacade;
    private final RedisRateLimiter rateLimiter;
    private final CurrentUserProvider currentUserProvider;
    private final SearchExperimentRouter experimentRouter;

    @Value("${roomwallah.search.rate-limit.search-per-minute:60}")
    private int searchRateLimit;

    @Value("${roomwallah.search.rate-limit.autocomplete-per-minute:120}")
    private int autocompleteRateLimit;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_AUTOCOMPLETE_LIMIT = 10;
    private static final int DEFAULT_TRENDING_LIMIT = 10;
    private static final int MAX_TRENDING_LIMIT = 50;

    @GetMapping
    @Operation(summary = "Search properties with filters, sorting, and cursor-based pagination")
    public ApiResponse<SearchResponseDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) String listingPurpose,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) Boolean petFriendly,
            @RequestParam(required = false) Boolean ownerVerified,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean explain,
            @RequestParam(required = false) String experimentalBucket,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest
    ) {
        String correlationId = MDC.get("correlationId");
        String rateLimitKey = resolveRateLimitKey(httpRequest);

        if (!rateLimiter.isAllowed("search:" + rateLimitKey, searchRateLimit, 60)) {
            log.warn("Search rate limit exceeded for key: {}, correlationId: {}", rateLimitKey, correlationId);
            throw new RateLimitExceededException("Search rate limit exceeded. Please try again later.");
        }

        // Input Sanitization
        String sanitizedQ = q;
        if (q != null) {
            sanitizedQ = q.replace("%", "").replace("_", "").trim();
            if (sanitizedQ.length() > 100) {
                throw new IllegalArgumentException("Search query text too long (max 100 characters)");
            }
            if (sanitizedQ.isBlank() && !q.isBlank()) {
                throw new IllegalArgumentException("Invalid search query consisting only of wildcards");
            }
        }

        // Constraint validation
        if (radiusKm != null && radiusKm > 50.0) {
            throw new IllegalArgumentException("Search radius cannot exceed 50 km");
        }

        log.info("Search request received - q: {}, city: {}, correlationId: {}", sanitizedQ, city, correlationId);

        long startTime = System.currentTimeMillis();

        int pageSize = resolvePageSize(size);

        PriceRange priceRange = (minPrice != null || maxPrice != null)
                ? new PriceRange(minPrice, maxPrice)
                : null;

        GeoRadius geoRadius = (latitude != null && longitude != null && radiusKm != null)
                ? new GeoRadius(latitude, longitude, radiusKm)
                : null;

        SortOption sort = (sortBy != null)
                ? new SortOption(sortBy, !"desc".equalsIgnoreCase(sortDir))
                : null;

        SearchFilter filter = SearchFilter.builder()
                .city(city)
                .locality(locality)
                .propertyType(propertyType)
                .listingPurpose(listingPurpose)
                .priceRange(priceRange)
                .bedrooms(bedrooms)
                .bathrooms(bathrooms)
                .petFriendly(petFriendly)
                .ownerVerified(ownerVerified)
                .geoRadius(geoRadius)
                .build();

        UUID userId = resolveUserId();

        // Resolve experiment bucket
        String bucket = experimentalBucket;
        if (bucket == null || bucket.isBlank()) {
            bucket = experimentRouter.getBucket("hybridSearch", userId, deviceId).name();
        }
        log.info("Canary experiment allocation - bucket: {}, userId: {}, deviceId: {}, correlationId: {}",
                bucket, userId, deviceId, correlationId);

        searchFacade.trackTelemetry("canary.bucket_allocation");

        SearchQuery searchQuery = SearchQuery.builder()
                .text(sanitizedQ)
                .filter(filter)
                .sort(sort)
                .page(new CursorPage(cursor, pageSize))
                .explain(explain)
                .experimentalBucket(bucket)
                .build();

        SearchEnginePort.SearchResult result = searchFacade.search(searchQuery, userId, correlationId);

        long executionTimeMs = System.currentTimeMillis() - startTime;

        List<PropertyCardDto> cards = result.documents().stream()
                .map(this::toPropertyCard)
                .collect(Collectors.toList());

        SearchResponseDto response = SearchResponseDto.builder()
                .results(cards)
                .nextCursor(result.nextCursor())
                .totalCount(result.totalCount())
                .executionTimeMs(executionTimeMs)
                .build();

        log.info("Search completed - totalCount: {}, returnedCount: {}, executionTimeMs: {}, correlationId: {}",
                result.totalCount(), cards.size(), executionTimeMs, correlationId);

        return ApiResponse.success(response, "Search completed successfully");
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Get autocomplete suggestions for search queries")
    public ApiResponse<AutoCompleteResponseDto> autoComplete(
            @RequestParam String q,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        String correlationId = MDC.get("correlationId");
        String rateLimitKey = resolveRateLimitKey(httpRequest);

        if (!rateLimiter.isAllowed("autocomplete:" + rateLimitKey, autocompleteRateLimit, 60)) {
            log.warn("Autocomplete rate limit exceeded for key: {}, correlationId: {}", rateLimitKey, correlationId);
            throw new RateLimitExceededException("Autocomplete rate limit exceeded. Please try again later.");
        }

        int clampedLimit = Math.min(Math.max(limit, 1), DEFAULT_AUTOCOMPLETE_LIMIT);
        log.debug("Autocomplete request - q: {}, city: {}, limit: {}, correlationId: {}", q, city, clampedLimit, correlationId);

        searchFacade.trackTelemetry("autocomplete.requests");

        List<String> suggestions = searchFacade.autoComplete(q, city, clampedLimit);

        AutoCompleteResponseDto response = AutoCompleteResponseDto.builder()
                .suggestions(suggestions)
                .build();

        return ApiResponse.success(response, "Autocomplete suggestions retrieved successfully");
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending search queries, optionally filtered by city")
    public ApiResponse<List<TrendingQueryDto>> getTrending(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") int limit
    ) {
        String correlationId = MDC.get("correlationId");
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_TRENDING_LIMIT);

        log.debug("Trending queries request - city: {}, limit: {}, correlationId: {}", city, clampedLimit, correlationId);

        List<TrendingQuery> trendingQueries = searchFacade.getTrending(city, clampedLimit);

        List<TrendingQueryDto> response = trendingQueries.stream()
                .map(tq -> TrendingQueryDto.builder()
                        .queryText(tq.getQueryText())
                        .city(tq.getCity())
                        .searchCount(tq.getSearchCount())
                        .build())
                        .collect(Collectors.toList());

        return ApiResponse.success(response, "Trending queries retrieved successfully");
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

    private int resolvePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private UUID resolveUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return currentUserProvider.getCurrentUser().getId();
            }
        } catch (Exception e) {
            log.debug("Could not resolve user ID for search, proceeding as anonymous");
        }
        return null;
    }

    private String resolveRateLimitKey(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return "user:" + currentUserProvider.getCurrentUser().getId();
            }
        } catch (Exception e) {
            log.debug("Could not resolve user for rate limiting, falling back to IP");
        }
        if (request == null) {
            return "ip:unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
