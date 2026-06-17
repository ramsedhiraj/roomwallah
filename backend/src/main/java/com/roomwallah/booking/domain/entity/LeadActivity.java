package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "lead_activity")
@Getter
@Setter
public class LeadActivity extends BaseEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "activity_type", nullable = false, length = 100)
    private String activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
