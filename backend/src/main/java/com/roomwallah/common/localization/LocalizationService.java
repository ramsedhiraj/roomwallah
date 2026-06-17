package com.roomwallah.common.localization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalizationService {

    private final LocalizedTranslationRepository repository;

    @Cacheable(value = "localization", key = "#key + '_' + #locale")
    public String getTranslation(String key, String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            locale = "en-IN";
        }
        
        Optional<LocalizedTranslation> translation = repository.findByTranslationKeyAndLocale(key, locale);
        if (translation.isPresent()) {
            return translation.get().getTranslationValue();
        }

        // Fallback hierarchy: mr-IN -> hi-IN -> en-IN
        if ("mr-IN".equalsIgnoreCase(locale)) {
            return getTranslation(key, "hi-IN");
        } else if ("hi-IN".equalsIgnoreCase(locale)) {
            return getTranslation(key, "en-IN");
        }

        log.warn("Translation key not found: {} for locale: {}", key, locale);
        return key; // return key as fallback
    }
}
