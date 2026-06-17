package com.roomwallah.property.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.property.application.facade.PropertyFacade;
import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.property.presentation.dto.PropertyResponse;
import com.roomwallah.property.presentation.dto.UpdatePropertyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property domain and listing management endpoints")
public class PropertyController {

    private final PropertyFacade propertyFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new draft property listing")
    public ApiResponse<PropertyResponse> createDraft(@Valid @RequestBody CreatePropertyRequest request) {
        log.info("Received request to create property draft: {}", request.getTitle());
        PropertyResponse response = propertyFacade.createDraft(request);
        return ApiResponse.success(response, "Property draft created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update property listing details")
    public ApiResponse<PropertyResponse> updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        log.info("Received request to update property: {}", id);
        PropertyResponse response = propertyFacade.updateProperty(id, request);
        return ApiResponse.success(response, "Property updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property listing details by ID")
    public ApiResponse<PropertyResponse> getPropertyById(@PathVariable UUID id) {
        log.info("Received request to fetch property by ID: {}", id);
        PropertyResponse response = propertyFacade.getPropertyById(id);
        return ApiResponse.success(response, "Property retrieved successfully");
    }

    @GetMapping("/me")
    @Operation(summary = "Get property listings of the current authenticated user")
    public ApiResponse<List<PropertyResponse>> getMyProperties() {
        log.info("Received request to fetch current owner's properties");
        List<PropertyResponse> response = propertyFacade.getMyProperties();
        return ApiResponse.success(response, "Properties retrieved successfully");
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit property listing for verification")
    public ApiResponse<PropertyResponse> submitForVerification(@PathVariable UUID id) {
        log.info("Received request to submit property for verification: {}", id);
        PropertyResponse response = propertyFacade.submitForVerification(id);
        return ApiResponse.success(response, "Property submitted for verification successfully");
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Approve verification and publish property listing")
    public ApiResponse<PropertyResponse> approveAndPublish(@PathVariable UUID id) {
        log.info("Received request to approve and publish property: {}", id);
        PropertyResponse response = propertyFacade.approveAndPublish(id);
        return ApiResponse.success(response, "Property published successfully");
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause an active property listing")
    public ApiResponse<PropertyResponse> pauseListing(@PathVariable UUID id) {
        log.info("Received request to pause property listing: {}", id);
        PropertyResponse response = propertyFacade.pauseListing(id);
        return ApiResponse.success(response, "Property listing paused successfully");
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive property listing")
    public ApiResponse<PropertyResponse> archiveListing(@PathVariable UUID id) {
        log.info("Received request to archive property listing: {}", id);
        PropertyResponse response = propertyFacade.archiveListing(id);
        return ApiResponse.success(response, "Property listing archived successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete property listing")
    public ApiResponse<Void> deleteProperty(@PathVariable UUID id) {
        log.info("Received request to soft delete property: {}", id);
        propertyFacade.deleteProperty(id);
        return ApiResponse.success("Property deleted successfully");
    }
}
