package com.roomwallah.searcheval;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/search/evaluations")
@RequiredArgsConstructor
@Tag(name = "Search Evaluation Framework", description = "Admin endpoints for monitoring and calculating search quality (NDCG, CTR, precision)")
public class SearchEvaluationController {

    private final SearchEvaluationService searchEvaluationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger search quality metric calculations and persist results")
    public ApiResponse<SearchEvaluation> triggerEvaluation() {
        log.info("Admin triggered manual search quality evaluation run");
        SearchEvaluation eval = searchEvaluationService.evaluateAndLogSearchQuality();
        return ApiResponse.success(eval, "Search evaluation completed and saved");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get historical search quality evaluation logs")
    public ApiResponse<List<SearchEvaluation>> getHistory() {
        log.info("Admin requested list of historical search quality evaluations");
        List<SearchEvaluation> history = searchEvaluationService.getHistoricalEvaluations();
        return ApiResponse.success(history, "Historical search evaluations retrieved successfully");
    }
}
