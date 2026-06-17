package com.roomwallah.trust.presentation;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.trust.application.facade.TrustFacade;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.presentation.dto.VerificationSubmissionRequest;
import com.roomwallah.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/trust")
public class TrustController {

    private final TrustFacade trustFacade;
    private final CurrentUserProvider currentUserProvider;

    public TrustController(TrustFacade trustFacade, CurrentUserProvider currentUserProvider) {
        this.trustFacade = trustFacade;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/verification")
    public ApiResponse<OwnerVerification> submitVerification(
            @Valid @RequestBody VerificationSubmissionRequest request,
            HttpServletRequest httpServletRequest) {
        User user = currentUserProvider.getCurrentUser();
        log.info("TrustController: user {} submitted verification", user.getId());

        String ipAddress = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        // Analyze network risk before submission
        trustFacade.analyzeNetworkRisk(user.getId(), ipAddress, userAgent);

        OwnerVerification ov = trustFacade.submitVerification(
                user.getId(),
                request.getLevel(),
                request.getProvider(),
                request.getMediaIds(),
                null
        );

        return ApiResponse.success(ov, "Verification request submitted successfully");
    }

    @GetMapping("/status")
    public ApiResponse<OwnerVerification> getStatus() {
        User user = currentUserProvider.getCurrentUser();
        log.info("TrustController: get verification status for user {}", user.getId());
        OwnerVerification ov = trustFacade.getOwnerVerification(user.getId())
                .orElse(null);
        return ApiResponse.success(ov, "Verification status retrieved successfully");
    }

    @GetMapping("/score")
    public ApiResponse<TrustScore> getScore() {
        User user = currentUserProvider.getCurrentUser();
        log.info("TrustController: get trust score for user {}", user.getId());
        TrustScore ts = trustFacade.getTrustScore(user.getId())
                .orElse(null);
        return ApiResponse.success(ts, "Trust score retrieved successfully");
    }

    @GetMapping("/users/{id}/trust")
    public ApiResponse<TrustScore> getUserTrust(@PathVariable("id") UUID userId) {
        log.info("TrustController: get trust score for target user {}", userId);
        TrustScore ts = trustFacade.getTrustScore(userId)
                .orElse(null);
        return ApiResponse.success(ts, "Trust score retrieved successfully");
    }
}
