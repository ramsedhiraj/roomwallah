package com.roomwallah.booking.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "booking_history")
@Getter
@Setter
public class BookingHistory extends BaseEntity {

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "status_from", length = 50)
    private String statusFrom;

    @Column(name = "status_to", nullable = false, length = 50)
    private String statusTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
