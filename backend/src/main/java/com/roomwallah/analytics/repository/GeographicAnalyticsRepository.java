package com.roomwallah.analytics.repository;

import com.roomwallah.analytics.domain.GeographicAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeographicAnalyticsRepository extends JpaRepository<GeographicAnalytics, UUID> {
    List<GeographicAnalytics> findBySnapshotDate(LocalDate date);
    Optional<GeographicAnalytics> findBySnapshotDateAndCityAndCountry(LocalDate date, String city, String country);
}
