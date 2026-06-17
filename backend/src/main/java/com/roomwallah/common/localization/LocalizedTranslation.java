package com.roomwallah.common.localization;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "localized_translations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizedTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "translation_key", nullable = false)
    private String translationKey;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "translation_value", nullable = false, columnDefinition = "TEXT")
    private String translationValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
