package com.roomwallah.search.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.search.application.facade.SearchFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@Tag(name = "Search Admin", description = "Search index administration and monitoring (ADMIN only)")
public class SearchAdminController {

    private final SearchFacade searchFacade;

    @PostMapping("/reindex")
    @Operation(summary = "Trigger a full reindex of all properties into the search index")
    public ApiResponse<Map<String, Object>> reindexAll() {
        String correlationId = MDC.get("correlationId");
        log.info("Full reindex triggered - correlationId: {}", correlationId);

        long count = searchFacade.reindexAll();

        log.info("Full reindex completed - documentCount: {}, correlationId: {}", count, correlationId);

        return ApiResponse.success(
                Map.of("documentsIndexed", count, "correlationId", correlationId),
                "Full reindex completed successfully"
        );
    }

    @PostMapping("/reindex/incremental")
    @Operation(summary = "Trigger an incremental reindex for properties updated since the given timestamp")
    public ApiResponse<Map<String, Object>> reindexIncremental(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        String correlationId = MDC.get("correlationId");
        log.info("Incremental reindex triggered - since: {}, correlationId: {}", since, correlationId);

        long count = searchFacade.reindexIncremental(since);

        log.info("Incremental reindex completed - since: {}, documentCount: {}, correlationId: {}",
                since, count, correlationId);

        return ApiResponse.success(
                Map.of("documentsIndexed", count, "since", since.toString(), "correlationId", correlationId),
                "Incremental reindex completed successfully"
        );
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Reconcile search index with source-of-truth to repair drift")
    public ApiResponse<Map<String, Object>> reconcile() {
        String correlationId = MDC.get("correlationId");
        log.info("Index reconciliation triggered - correlationId: {}", correlationId);

        long count = searchFacade.reconcile();

        log.info("Index reconciliation completed - repairedCount: {}, correlationId: {}", count, correlationId);

        return ApiResponse.success(
                Map.of("documentsRepaired", count, "correlationId", correlationId),
                "Index reconciliation completed successfully"
        );
    }

    @GetMapping("/health")
    @Operation(summary = "Get search index health status")
    public ApiResponse<Map<String, Object>> getHealth() {
        String correlationId = MDC.get("correlationId");
        log.debug("Index health check requested - correlationId: {}", correlationId);

        Map<String, Object> health = searchFacade.getIndexHealth();

        return ApiResponse.success(health, "Index health retrieved successfully");
    }

    @GetMapping("/stats")
    @Operation(summary = "Get search index statistics")
    public ApiResponse<Map<String, Object>> getStats() {
        String correlationId = MDC.get("correlationId");
        log.debug("Index stats requested - correlationId: {}", correlationId);

        Map<String, Object> stats = searchFacade.getIndexStats();

        return ApiResponse.success(stats, "Index statistics retrieved successfully");
    }

    @PostMapping("/maintenance")
    @Operation(summary = "Run search maintenance tasks (drift reconciliation, recommendation cache evictions)")
    public ApiResponse<Map<String, Object>> executeMaintenance() {
        String correlationId = MDC.get("correlationId");
        log.info("Administrative search maintenance tasks triggered - correlationId: {}", correlationId);

        searchFacade.executeMaintenanceTasks();

        return ApiResponse.success(
                Map.of("status", "SUCCESS", "correlationId", correlationId),
                "Search maintenance tasks completed successfully"
        );
    }
}
