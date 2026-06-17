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
@Table(name = "trending_queries")
@Getter
@Setter
public class TrendingQuery {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "query_text", nullable = false, length = 255)
    private String queryText;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "search_count", nullable = false)
    private int searchCount;

    @Column(name = "last_aggregated_at", nullable = false)
    private Instant lastAggregatedAt;
}
