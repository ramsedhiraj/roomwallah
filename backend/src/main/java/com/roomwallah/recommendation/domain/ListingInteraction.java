package com.roomwallah.recommendation.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingInteraction extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "interaction_type", nullable = false)
    private String interactionType;

    @Column(name = "interaction_time", nullable = false)
    private Instant interactionTime;
}
