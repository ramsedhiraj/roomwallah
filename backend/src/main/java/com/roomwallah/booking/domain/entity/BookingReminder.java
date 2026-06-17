package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_reminders")
@Getter
@Setter
public class BookingReminder extends BaseEntity {

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "trigger_at", nullable = false)
    private Instant triggerAt;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ReminderType type;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
