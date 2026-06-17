package com.roomwallah.verification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_provider_metrics")
@Getter
@Setter
public class VerificationProviderMetrics {

    @Id
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "total_requests", nullable = false)
    private long totalRequests = 0;

    @Column(name = "successful_requests", nullable = false)
    private long successfulRequests = 0;

    @Column(name = "failed_requests", nullable = false)
    private long failedRequests = 0;

    @Column(name = "timeout_requests", nullable = false)
    private long timeoutRequests = 0;

    @Column(name = "average_latency_ms", nullable = false)
    private long averageLatencyMs = 0;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
