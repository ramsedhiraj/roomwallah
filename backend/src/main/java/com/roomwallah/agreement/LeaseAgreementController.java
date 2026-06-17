package com.roomwallah.agreement;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
@Tag(name = "E-Sign & Lease Lifecycle", description = "Endpoints for managing, generating, and digitally signing lease agreements")
public class LeaseAgreementController {

    private final LeaseAgreementService agreementService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Draft a new lease agreement")
    public ApiResponse<LeaseAgreement> createAgreement(@RequestBody Map<String, Object> payload) {
        UUID propertyId = UUID.fromString((String) payload.get("propertyId"));
        UUID tenantId = UUID.fromString((String) payload.get("tenantId"));
        UUID ownerId = UUID.fromString((String) payload.get("ownerId"));
        BigDecimal rentAmount = new BigDecimal(payload.get("rentAmount").toString());
        LocalDate start = LocalDate.parse((String) payload.get("startDate"));
        LocalDate end = LocalDate.parse((String) payload.get("endDate"));
        String content = (String) payload.get("agreementContent");

        LeaseAgreement agreement = agreementService.createAgreement(
                propertyId, tenantId, ownerId, rentAmount, start, end, content
        );
        return ApiResponse.success(agreement, "Lease agreement drafted successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lease agreement details by ID")
    public ApiResponse<LeaseAgreement> getAgreement(@PathVariable UUID id) {
        return ApiResponse.success(agreementService.getAgreement(id), "Lease agreement retrieved successfully");
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "Digitally sign a lease agreement")
    public ApiResponse<LeaseAgreement> signAgreement(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String hash = payload.get("signatureHash");
        String fingerprint = payload.getOrDefault("deviceFingerprint", "unknown");
        String ip = request.getRemoteAddr();

        LeaseAgreement agreement = agreementService.signAgreement(id, userId, hash, ip, fingerprint);
        return ApiResponse.success(agreement, "Lease agreement signed successfully");
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew an existing lease agreement")
    public ApiResponse<LeaseAgreement> renewAgreement(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload
    ) {
        LocalDate newEnd = LocalDate.parse(payload.get("newEndDate"));
        LeaseAgreement agreement = agreementService.renewAgreement(id, newEnd);
        return ApiResponse.success(agreement, "Lease agreement renewed successfully");
    }

    @PostMapping("/{id}/amend")
    @Operation(summary = "Amend an existing lease agreement")
    public ApiResponse<LeaseAgreement> amendAgreement(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload
    ) {
        String newContent = payload.get("newContent");
        LeaseAgreement agreement = agreementService.amendAgreement(id, newContent);
        return ApiResponse.success(agreement, "Lease agreement amended successfully");
    }

    @GetMapping("/tenant")
    @Operation(summary = "Get all agreements where the user is the tenant")
    public ApiResponse<List<LeaseAgreement>> getTenantAgreements() {
        UUID tenantId = currentUserProvider.getCurrentUser().getId();
        return ApiResponse.success(agreementService.getTenantAgreements(tenantId), "Tenant agreements retrieved successfully");
    }

    @GetMapping("/owner")
    @Operation(summary = "Get all agreements where the user is the owner")
    public ApiResponse<List<LeaseAgreement>> getOwnerAgreements() {
        UUID ownerId = currentUserProvider.getCurrentUser().getId();
        return ApiResponse.success(agreementService.getOwnerAgreements(ownerId), "Owner agreements retrieved successfully");
    }
}
