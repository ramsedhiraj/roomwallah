package com.roomwallah.trust.presentation;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.trust.application.facade.TrustFacade;
import com.roomwallah.trust.domain.entity.ModerationCase;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.presentation.dto.RecalculateRequest;
import com.roomwallah.trust.presentation.dto.RejectionRequest;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/trust")
public class TrustAdminController {

    private final TrustFacade trustFacade;
    private final CurrentUserProvider currentUserProvider;

    public TrustAdminController(TrustFacade trustFacade, CurrentUserProvider currentUserProvider) {
        this.trustFacade = trustFacade;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/cases")
    public ApiResponse<List<ModerationCase>> getOpenCases() {
        log.info("TrustAdminController: retrieving open moderation cases");
        List<ModerationCase> cases = trustFacade.getOpenCases();
        return ApiResponse.success(cases, "Open cases retrieved successfully");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<OwnerVerification> approveVerification(@PathVariable("id") UUID id) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("TrustAdminController: admin {} approved verification ID: {}", admin.getId(), id);
        OwnerVerification ov = trustFacade.approveVerification(id, admin.getId());
        return ApiResponse.success(ov, "Owner verification request approved");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<OwnerVerification> rejectVerification(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectionRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("TrustAdminController: admin {} rejected verification ID: {} with reason: {}", admin.getId(), id, request.getReason());
        OwnerVerification ov = trustFacade.rejectVerification(id, admin.getId(), request.getReason());
        return ApiResponse.success(ov, "Owner verification request rejected");
    }

    @PostMapping("/recalculate")
    public ApiResponse<TrustScore> recalculateTrustScore(@Valid @RequestBody RecalculateRequest request) {
        log.info("TrustAdminController: request to recalculate trust score for user: {}", request.getUserId());
        TrustScore ts = trustFacade.recalculateTrustScore(request.getUserId(), "ADMIN_TRIGGERED_RECALCULATION");
        return ApiResponse.success(ts, "Trust score successfully recalculated");
    }
}
