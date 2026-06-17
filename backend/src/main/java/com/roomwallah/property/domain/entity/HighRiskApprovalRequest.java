package com.roomwallah.property.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "high_risk_approval_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighRiskApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType; // REJECT_LISTING, SUSPEND_USER, BLOCK_OWNER, MODIFY_PRICE

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy; // SYSTEM_AI, USER_REPORT

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "proposed_data", columnDefinition = "TEXT")
    private String proposedData; // Proposed changes as JSON

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Version
    private Long version;
}
