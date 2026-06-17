package com.roomwallah.property.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "suspected_duplicate_clusters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspectedDuplicateCluster {

    @Id
    @Column(name = "id", length = 100)
    private String id; // cluster-1, cluster-2 etc

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Column(name = "locality")
    private String locality;

    @Column(name = "city")
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_a_id", nullable = false)
    private Property candidateA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_b_id", nullable = false)
    private Property candidateB;

    @Column(name = "match_insights", columnDefinition = "TEXT")
    private String matchInsights;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, RESOLVED_MERGED, RESOLVED_DISMISSED, RESOLVED_SUSPENDED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
