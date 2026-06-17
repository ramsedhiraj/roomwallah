package com.roomwallah.graph;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
@Tag(name = "Property Knowledge Graph", description = "Endpoints for traversing relationships between listings, owners, and amenities")
public class PropertyGraphController {

    private final PropertyGraphService propertyGraphService;

    @GetMapping("/search")
    @Operation(summary = "Traverse the knowledge graph and return explainable recommendation paths for a property")
    public ApiResponse<PropertyGraphService.GraphTraversalResult> search(@RequestParam UUID propertyId) {
        log.info("Received request to traverse property knowledge graph for ID: {}", propertyId);
        PropertyGraphService.GraphTraversalResult result = propertyGraphService.traverseGraph(propertyId);
        return ApiResponse.success(result, "Graph traversal complete");
    }
}
