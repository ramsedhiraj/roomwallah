package com.roomwallah.search.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.search.application.facade.SearchFacade;
import com.roomwallah.search.domain.entity.SavedSearch;
import com.roomwallah.search.presentation.dto.SavedSearchRequestDto;
import com.roomwallah.search.presentation.dto.SavedSearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/search/saved")
@RequiredArgsConstructor
@Tag(name = "Saved Searches", description = "Manage saved search queries with optional notifications")
public class SavedSearchController {

    private final SearchFacade searchFacade;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Create a new saved search for the authenticated user")
    public ApiResponse<SavedSearchResponseDto> createSavedSearch(
            @Valid @RequestBody SavedSearchRequestDto request
    ) {
        String correlationId = MDC.get("correlationId");
        UUID userId = currentUserProvider.getCurrentUser().getId();

        log.info("Create saved search request - userId: {}, notificationEnabled: {}, correlationId: {}",
                userId, request.isNotificationEnabled(), correlationId);

        SavedSearch savedSearch = searchFacade.createSavedSearch(
                userId,
                request.getSerializedQuery(),
                request.isNotificationEnabled()
        );

        SavedSearchResponseDto response = toResponseDto(savedSearch);

        log.info("Saved search created - id: {}, userId: {}, correlationId: {}", savedSearch.getId(), userId, correlationId);

        return ApiResponse.success(response, "Saved search created successfully");
    }

    @GetMapping
    @Operation(summary = "List all saved searches for the authenticated user")
    public ApiResponse<List<SavedSearchResponseDto>> getSavedSearches() {
        String correlationId = MDC.get("correlationId");
        UUID userId = currentUserProvider.getCurrentUser().getId();

        log.debug("Get saved searches request - userId: {}, correlationId: {}", userId, correlationId);

        List<SavedSearch> savedSearches = searchFacade.getSavedSearches(userId);

        List<SavedSearchResponseDto> response = savedSearches.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        log.debug("Saved searches retrieved - userId: {}, count: {}, correlationId: {}", userId, response.size(), correlationId);

        return ApiResponse.success(response, "Saved searches retrieved successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved search by ID for the authenticated user")
    public ApiResponse<Void> deleteSavedSearch(@PathVariable UUID id) {
        String correlationId = MDC.get("correlationId");
        UUID userId = currentUserProvider.getCurrentUser().getId();

        log.info("Delete saved search request - id: {}, userId: {}, correlationId: {}", id, userId, correlationId);

        searchFacade.deleteSavedSearch(id, userId);

        log.info("Saved search deleted - id: {}, userId: {}, correlationId: {}", id, userId, correlationId);

        return ApiResponse.success("Saved search deleted successfully");
    }

    private SavedSearchResponseDto toResponseDto(SavedSearch savedSearch) {
        return SavedSearchResponseDto.builder()
                .id(savedSearch.getId())
                .serializedQuery(savedSearch.getSerializedQuery())
                .notificationEnabled(savedSearch.isNotificationEnabled())
                .lastTriggeredAt(savedSearch.getLastTriggeredAt())
                .createdAt(savedSearch.getCreatedAt())
                .build();
    }
}
