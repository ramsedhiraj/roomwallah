package com.roomwallah.analytics.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hourly_metrics_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyMetricsSnapshot extends BaseEntity {

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "snapshot_hour", nullable = false)
    private int snapshotHour;

    @Column(name = "total_bookings", nullable = false)
    private int totalBookings;

    @Column(name = "active_listings", nullable = false)
    private int activeListings;

    @Column(nullable = false)
    private BigDecimal revenue;

    @Column(name = "occupancy_rate", nullable = false)
    private BigDecimal occupancyRate;
}
