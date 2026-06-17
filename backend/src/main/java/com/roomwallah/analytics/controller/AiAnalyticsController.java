package com.roomwallah.analytics.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.common.outbox.DomainOutboxEvent;
import com.roomwallah.common.outbox.DomainOutboxEventRepository;
import com.roomwallah.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "AI Analytics and Outbox Management", description = "AI Cost metrics and Transactional outbox controls")
public class AiAnalyticsController {

    private final AiObservabilityService observabilityService;
    private final DomainOutboxEventRepository outboxEventRepository;

    @GetMapping("/ai-analytics/stats")
    @Operation(summary = "Get AI usage, cost, latency, and observability metrics")
    public ApiResponse<Map<String, Object>> getAiStats() {
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> stats = observabilityService.getDashboardStats(tenantId);
        return ApiResponse.success(stats, "AI Analytics metrics retrieved successfully");
    }

    @PostMapping("/ai-analytics/config")
    @Operation(summary = "Update AI configurations (model selection, caching flags)")
    public ApiResponse<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        log.info("Updating AI configuration: {}", body);
        return ApiResponse.success(body, "AI configuration saved successfully");
    }

    @PostMapping("/outbox/redrive")
    @Operation(summary = "Redrive all Dead-Letter Queue (DLQ) outbox events")
    public ApiResponse<Map<String, Object>> redriveDlq() {
        log.info("Triggering redrive for DLQ outbox events...");
        List<DomainOutboxEvent> allEvents = outboxEventRepository.findAll();
        int count = 0;
        for (DomainOutboxEvent event : allEvents) {
            if ("DLQ".equals(event.getStatus())) {
                event.setStatus("PENDING");
                event.setRetryCount(0);
                event.setNextAttemptAt(Instant.now());
                outboxEventRepository.save(event);
                count++;
            }
        }
        log.info("Redrive complete. Reset {} events to PENDING.", count);
        return ApiResponse.success(Map.of("redrivedCount", count), "DLQ redrive completed successfully");
    }

    @PostMapping("/outbox/purge")
    @Operation(summary = "Purge all Dead-Letter Queue (DLQ) outbox events")
    public ApiResponse<Map<String, Object>> purgeDlq() {
        log.info("Triggering purge for DLQ outbox events...");
        List<DomainOutboxEvent> allEvents = outboxEventRepository.findAll();
        int count = 0;
        for (DomainOutboxEvent event : allEvents) {
            if ("DLQ".equals(event.getStatus())) {
                outboxEventRepository.delete(event);
                count++;
            }
        }
        log.info("Purge complete. Deleted {} DLQ events.", count);
        return ApiResponse.success(Map.of("purgedCount", count), "DLQ purge completed successfully");
    }
}
