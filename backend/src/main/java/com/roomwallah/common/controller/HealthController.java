package com.roomwallah.common.controller;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System health check endpoint")
public class HealthController {

    @GetMapping
    @Operation(summary = "Get service health status")
    public ApiResponse<Map<String, String>> checkHealth() {
        Map<String, String> healthData = Map.of(
                "status", "UP",
                "service", "RoomWallah API",
                "version", "1.0.0"
        );
        return ApiResponse.success(healthData, "Service is running");
    }
}
