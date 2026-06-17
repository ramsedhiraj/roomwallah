package com.roomwallah.common.localization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LocalizedTranslationRepository extends JpaRepository<LocalizedTranslation, UUID> {
    Optional<LocalizedTranslation> findByTranslationKeyAndLocale(String translationKey, String locale);
}
