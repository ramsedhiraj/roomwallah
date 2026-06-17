package com.roomwallah.common.ai.feedback;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/feedback")
@RequiredArgsConstructor
@Tag(name = "AI Feedback Collector", description = "Collect human ratings and issue reports for AI responses")
public class AiFeedbackController {

    private final AiFeedbackRepository feedbackRepository;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Submit human thumbs up/down rating or issue report on an AI output")
    public ApiResponse<AiFeedback> submitFeedback(@RequestBody Map<String, Object> body) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        
        String targetType = (String) body.get("targetType");
        String targetId = (String) body.get("targetId");
        boolean isPositive = (Boolean) body.get("isPositive");
        String issueReport = (String) body.get("issueReport");

        if (targetType == null || targetId == null) {
            throw new IllegalArgumentException("targetType and targetId parameters are required");
        }

        AiFeedback feedback = AiFeedback.builder()
                .targetType(targetType.toUpperCase())
                .targetId(targetId)
                .userId(userId)
                .isPositive(isPositive)
                .issueReport(issueReport)
                .createdAt(Instant.now())
                .build();

        AiFeedback saved = feedbackRepository.save(feedback);
        log.info("Collected human feedback for target: {} (positive={})", targetId, isPositive);
        return ApiResponse.success(saved, "AI response feedback registered successfully");
    }
}
