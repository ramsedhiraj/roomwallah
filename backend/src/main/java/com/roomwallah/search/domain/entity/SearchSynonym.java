package com.roomwallah.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_synonyms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSynonym {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "term", nullable = false, unique = true, length = 100)
    private String term;

    @Column(name = "synonyms", nullable = false, columnDefinition = "TEXT")
    private String synonyms;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
