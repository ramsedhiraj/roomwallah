package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "booking_waitlist")
@Getter
@Setter
public class WaitlistEntry extends BaseEntity {

    @Column(name = "visit_slot_id", nullable = false)
    private UUID visitSlotId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
