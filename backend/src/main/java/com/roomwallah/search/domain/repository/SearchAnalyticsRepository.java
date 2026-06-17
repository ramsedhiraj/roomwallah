package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.SearchAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchAnalytics, UUID> {

    void deleteByCreatedAtBefore(Instant cutoff);

    long countByCreatedAtAfter(Instant since);

    List<SearchAnalytics> findTop100ByOrderByCreatedAtDesc();

    List<SearchAnalytics> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
