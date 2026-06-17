package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "lead_notes")
@Getter
@Setter
public class LeadNote extends BaseEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
