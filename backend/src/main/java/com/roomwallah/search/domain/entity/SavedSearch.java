package com.roomwallah.search.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_searches")
@Getter
@Setter
public class SavedSearch {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "serialized_query", nullable = false, columnDefinition = "TEXT")
    private String serializedQuery;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = false;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "notification_frequency", length = 50)
    private String notificationFrequency = "INSTANT";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
