package com.roomwallah.analytics.repository;

import com.roomwallah.analytics.domain.HourlyMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HourlyMetricsSnapshotRepository extends JpaRepository<HourlyMetricsSnapshot, UUID> {
    List<HourlyMetricsSnapshot> findBySnapshotDate(LocalDate date);
    Optional<HourlyMetricsSnapshot> findBySnapshotDateAndSnapshotHour(LocalDate date, int hour);
}
