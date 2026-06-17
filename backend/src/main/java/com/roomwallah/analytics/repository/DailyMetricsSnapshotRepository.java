package com.roomwallah.analytics.repository;

import com.roomwallah.analytics.domain.DailyMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMetricsSnapshotRepository extends JpaRepository<DailyMetricsSnapshot, UUID> {
    Optional<DailyMetricsSnapshot> findBySnapshotDate(LocalDate date);
}
