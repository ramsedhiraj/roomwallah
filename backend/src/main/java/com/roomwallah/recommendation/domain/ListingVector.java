package com.roomwallah.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_vectors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingVector {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "embedding", nullable = false)
    private double[] embedding;

    @Column(name = "embedding_version", nullable = false)
    private int embeddingVersion;

    @Column(name = "model_identifier", nullable = false, length = 100)
    private String modelIdentifier;

    @Column(name = "generation_timestamp", nullable = false)
    private Instant generationTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
