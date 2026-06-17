package com.roomwallah.analytics.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_metrics_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyMetricsSnapshot extends BaseEntity {

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "total_bookings", nullable = false)
    private int totalBookings;

    @Column(name = "active_listings", nullable = false)
    private int activeListings;

    @Column(nullable = false)
    private BigDecimal revenue;

    @Column(name = "occupancy_rate", nullable = false)
    private BigDecimal occupancyRate;

    @Column(name = "rolling_7d_bookings", nullable = false)
    private int rolling7dBookings;

    @Column(name = "rolling_7d_revenue", nullable = false)
    private BigDecimal rolling7dRevenue;

    @Column(name = "rolling_30d_bookings", nullable = false)
    private int rolling30dBookings;

    @Column(name = "rolling_30d_revenue", nullable = false)
    private BigDecimal rolling30dRevenue;
}
