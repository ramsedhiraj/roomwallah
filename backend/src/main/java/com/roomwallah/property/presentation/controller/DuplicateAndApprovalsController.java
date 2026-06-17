package com.roomwallah.property.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.property.domain.entity.HighRiskApprovalRequest;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.SuspectedDuplicateCluster;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.repository.SuspectedDuplicateClusterRepository;
import com.roomwallah.property.application.service.HighRiskAdminApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "AI Duplicates Moderation and High Risk Decision queue")
public class DuplicateAndApprovalsController {

    private final SuspectedDuplicateClusterRepository clusterRepository;
    private final PropertyRepository propertyRepository;
    private final HighRiskAdminApprovalService highRiskAdminApprovalService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/duplicates/{clusterId}/resolve")
    @Operation(summary = "Resolve a duplicate listing case group")
    public ApiResponse<Map<String, Object>> resolveDuplicate(
            @PathVariable String clusterId,
            @RequestBody Map<String, String> body
    ) {
        String action = body.get("action"); // merge, dismiss, flag
        if (action == null) {
            throw new IllegalArgumentException("Action type cannot be empty");
        }

        SuspectedDuplicateCluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("Duplicate cluster not found"));

        Property candidateB = cluster.getCandidateB();
        String responseMessage = "";

        if ("merge".equalsIgnoreCase(action)) {
            cluster.setStatus("RESOLVED_MERGED");
            candidateB.setDeleted(true);
            candidateB.setDeletedAt(Instant.now());
            propertyRepository.save(candidateB);
            responseMessage = "Duplicate listings merged successfully.";
        } else if ("dismiss".equalsIgnoreCase(action)) {
            cluster.setStatus("RESOLVED_DISMISSED");
            candidateB.setModerationStatus("APPROVED");
            candidateB.setModerationReason(null);
            propertyRepository.save(candidateB);
            responseMessage = "Duplicate case dismissed. Listing whitelisted.";
        } else if ("flag".equalsIgnoreCase(action)) {
            cluster.setStatus("RESOLVED_SUSPENDED");
            candidateB.setStatus(PropertyStatus.ARCHIVED); // suspended state
            candidateB.setModerationStatus("SUSPENDED_DUPLICATE");
            candidateB.setModerationReason("Listing suspended due to duplicate match.");
            propertyRepository.save(candidateB);
            responseMessage = "Newer listing suspended due to duplicate match.";
        }

        cluster.setUpdatedAt(Instant.now());
        clusterRepository.save(cluster);

        return ApiResponse.success(
                Map.of("clusterId", clusterId, "status", cluster.getStatus()), 
                responseMessage
        );
    }

    @GetMapping("/high-risk-approvals")
    @Operation(summary = "Get list of pending high-risk AI decisions requiring human approval")
    public ApiResponse<List<HighRiskApprovalRequest>> getPendingApprovals() {
        List<HighRiskApprovalRequest> list = highRiskAdminApprovalService.getPendingRequests();
        return ApiResponse.success(list, "Pending approvals retrieved successfully");
    }

    @PostMapping("/high-risk-approvals/{requestId}/resolve")
    @Operation(summary = "Approve or reject a high-risk AI action request")
    public ApiResponse<Map<String, Object>> resolveHighRiskAction(
            @PathVariable UUID requestId,
            @RequestBody Map<String, String> body
    ) {
        String action = body.get("action"); // approve, reject
        if (action == null) {
            throw new IllegalArgumentException("Action must be approve or reject");
        }

        String adminName = currentUserProvider.getCurrentUser().getEmail();

        if ("approve".equalsIgnoreCase(action)) {
            highRiskAdminApprovalService.approveRequest(requestId, adminName);
        } else {
            highRiskAdminApprovalService.rejectRequest(requestId, adminName);
        }

        return ApiResponse.success(
                Map.of("requestId", requestId, "result", action.toUpperCase()), 
                "High-risk action resolved successfully: " + action
        );
    }
}
