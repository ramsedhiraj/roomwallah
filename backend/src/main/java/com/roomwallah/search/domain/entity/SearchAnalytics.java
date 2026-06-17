package com.roomwallah.search.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_analytics")
@Getter
@Setter
public class SearchAnalytics {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "search_text", length = 255)
    private String searchText;

    @Column(name = "filters_json", columnDefinition = "TEXT")
    private String filtersJson;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
