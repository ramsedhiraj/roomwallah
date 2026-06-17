package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_assignments")
@Getter
@Setter
public class LeadAssignment extends BaseEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
