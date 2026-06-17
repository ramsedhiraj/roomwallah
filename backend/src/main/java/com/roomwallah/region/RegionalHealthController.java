package com.roomwallah.region;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
@Tag(name = "Regional Failover & Health", description = "Admin endpoints for monitoring and triggering storage bucket failover events")
public class RegionalHealthController {

    private final MultiRegionService regionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get the health status and routing configuration of all storage regions")
    public ApiResponse<Map<String, Boolean>> getStatus() {
        return ApiResponse.success(regionService.getRegionsStatus(), "Regional health statuses retrieved");
    }

    @PostMapping("/{region}/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set health status for a specific storage region (triggering failover if false)")
    public ApiResponse<Void> setHealth(
            @PathVariable String region,
            @RequestBody Map<String, Boolean> payload
    ) {
        boolean healthy = payload.getOrDefault("healthy", true);
        regionService.setRegionHealth(region, healthy);
        return ApiResponse.success("Region health status updated successfully");
    }
}
