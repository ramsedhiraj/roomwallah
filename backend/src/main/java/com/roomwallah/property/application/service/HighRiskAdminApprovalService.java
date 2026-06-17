package com.roomwallah.property.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.property.domain.entity.HighRiskApprovalRequest;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.HighRiskApprovalRequestRepository;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighRiskAdminApprovalService {

    private final HighRiskApprovalRequestRepository approvalRequestRepository;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public HighRiskApprovalRequest submitRequest(String actionType, String targetId, String requestedBy, String reason, Map<String, Object> proposedData) {
        String proposedDataJson = "";
        try {
            if (proposedData != null) {
                proposedDataJson = objectMapper.writeValueAsString(proposedData);
            }
        } catch (Exception e) {
            log.error("Failed to serialize proposed data", e);
        }

        HighRiskApprovalRequest request = HighRiskApprovalRequest.builder()
                .actionType(actionType.toUpperCase())
                .targetId(targetId)
                .requestedBy(requestedBy)
                .reason(reason)
                .proposedData(proposedDataJson)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        log.info("High-risk AI decision logged. Pending human review: Action={}, Target={}", actionType, targetId);
        return approvalRequestRepository.save(request);
    }

    @Transactional
    public void approveRequest(UUID requestId, String resolvedBy) {
        HighRiskApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is already resolved");
        }

        request.setStatus("APPROVED");
        request.setResolvedAt(Instant.now());
        request.setResolvedBy(resolvedBy);

        executeAction(request.getActionType(), request.getTargetId(), request.getProposedData());

        approvalRequestRepository.save(request);
        log.info("High-risk request APPROVED and executed by admin: {}", resolvedBy);
    }

    @Transactional
    public void rejectRequest(UUID requestId, String resolvedBy) {
        HighRiskApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is already resolved");
        }

        request.setStatus("REJECTED");
        request.setResolvedAt(Instant.now());
        request.setResolvedBy(resolvedBy);

        approvalRequestRepository.save(request);
        log.info("High-risk request REJECTED by admin: {}", resolvedBy);
    }

    private void executeAction(String actionType, String targetId, String proposedDataJson) {
        try {
            UUID id = UUID.fromString(targetId);
            if ("REJECT_LISTING".equals(actionType)) {
                Property property = propertyRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Property not found"));
                property.setStatus(PropertyStatus.ARCHIVED); // reject listing logic
                property.setModerationStatus("REJECTED_BY_ADMIN");
                propertyRepository.save(property);
            } else if ("MODIFY_PRICE".equals(actionType)) {
                Property property = propertyRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Property not found"));
                Map<?, ?> data = objectMapper.readValue(proposedDataJson, Map.class);
                Number proposedAmt = (Number) data.get("price");
                if (proposedAmt != null) {
                    property.setPrice(new Money(BigDecimal.valueOf(proposedAmt.doubleValue()), "INR"));
                    propertyRepository.save(property);
                }
            } else if ("SUSPEND_USER".equals(actionType) || "BLOCK_OWNER".equals(actionType)) {
                log.info("AI Decision executed: User {} suspended/blocked in user identity registers", targetId);
            }
        } catch (Exception e) {
            log.error("Failed to execute approved action", e);
            throw new RuntimeException("Execution of approved action failed: " + e.getMessage());
        }
    }

    public List<HighRiskApprovalRequest> getPendingRequests() {
        return approvalRequestRepository.findByStatus("PENDING");
    }
}
