package com.roomwallah.common.ai.registry;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.common.ai.feedback.AiBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
@Tag(name = "AI Operations Console", description = "AI Rollbacks, prompt registers, and regression benchmarks")
public class AiOpsController {

    private final PromptRegistry promptRegistry;
    private final AiBenchmarkService benchmarkService;

    @PostMapping("/benchmark")
    @Operation(summary = "Execute automated benchmarks checking parser precision and regressions")
    public ApiResponse<Map<String, Object>> runEvaluations() {
        log.info("Starting regression benchmarking evaluations...");
        Map<String, Object> results = benchmarkService.runEvaluations();
        return ApiResponse.success(results, "AI Regressions Benchmark executed successfully");
    }

    @PostMapping("/prompts/rollback")
    @Operation(summary = "Rollback or set the active version of an AI prompt template")
    public ApiResponse<Map<String, String>> rollbackPrompt(@RequestBody Map<String, String> body) {
        String templateKey = body.get("templateKey");
        String version = body.get("version");

        if (templateKey == null || version == null) {
            throw new IllegalArgumentException("templateKey and version parameters are required");
        }

        promptRegistry.rollbackOrSetVersion(templateKey, version);
        
        return ApiResponse.success(
                Map.of("templateKey", templateKey, "activeVersion", version),
                "Switched active prompt template version successfully"
        );
    }

    @GetMapping("/prompts/status")
    @Operation(summary = "Get the current active prompt template versions")
    public ApiResponse<Map<String, String>> getPromptStatus() {
        Map<String, String> status = Map.of(
                "intent_parsing", promptRegistry.getActiveVersion("intent_parsing"),
                "assistant_chat", promptRegistry.getActiveVersion("assistant_chat")
        );
        return ApiResponse.success(status, "Prompt templates version status retrieved");
    }
}
