package com.roomwallah.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_intent_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIntentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "query_text", nullable = false, length = 255)
    private String queryText;

    @Column(name = "parsed_intent", columnDefinition = "TEXT")
    private String parsedIntent;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "prompt_template_version", length = 50)
    private String promptTemplateVersion;

    @Version
    private Long version;
}
