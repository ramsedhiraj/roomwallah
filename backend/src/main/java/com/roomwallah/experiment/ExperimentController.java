package com.roomwallah.experiment;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/experiments")
@RequiredArgsConstructor
@Tag(name = "Experimentation Platform", description = "Admin endpoints for starting, stopping and rolling back A/B experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all active and past A/B experiments")
    public ApiResponse<List<Map<String, Object>>> listAll() {
        log.info("Admin requested list of all experiments");
        return ApiResponse.success(experimentService.listAllExperiments(), "Experiments retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start or update an A/B experiment")
    public ApiResponse<Void> start(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        int percent = ((Number) payload.getOrDefault("treatmentPercent", 50)).intValue();
        log.info("Admin starting/updating experiment: {} with {}% treatment", name, percent);
        experimentService.startExperiment(name, percent);
        return ApiResponse.success("Experiment started successfully");
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stop an active A/B experiment")
    public ApiResponse<Void> stop(@PathVariable String name) {
        log.info("Admin stopping experiment: {}", name);
        experimentService.stopExperiment(name);
        return ApiResponse.success("Experiment stopped successfully");
    }

    @PostMapping("/{name}/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rollback an experiment, clearing all assignments")
    public ApiResponse<Void> rollback(@PathVariable String name) {
        log.info("Admin rolling back experiment: {}", name);
        experimentService.rollbackExperiment(name);
        return ApiResponse.success("Experiment rolled back successfully");
    }
}
