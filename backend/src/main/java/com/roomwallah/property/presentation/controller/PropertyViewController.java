package com.roomwallah.property.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Property Views", description = "Endpoints for tracking property view counts")
public class PropertyViewController {

    private final PropertyRepository propertyRepository;
    private final SearchDocumentRepository searchDocumentRepository;

    @PostMapping("/{id}/view")
    @Transactional
    @Operation(summary = "Increment the view count of a property listing")
    public ApiResponse<Void> incrementViewCount(@PathVariable("id") UUID id) {
        log.info("Incrementing view count for propertyId: {}", id);
        propertyRepository.incrementViewCount(id);
        searchDocumentRepository.incrementViewCount(id);
        return ApiResponse.success(null, "View count incremented successfully");
    }
}
