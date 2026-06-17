package com.roomwallah.common.ai.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptRegistry {

    private final Map<String, Map<String, String>> templates = new ConcurrentHashMap<>();
    private final Map<String, String> activeVersions = new ConcurrentHashMap<>();

    public PromptRegistry() {
        // Initialize default templates and versions
        registerTemplate("intent_parsing", "v1", "Parse the user's property search query: '${query}'. " +
                "Respond with a JSON object containing: 'intent' (e.g., SEARCH), 'city', 'locality', " +
                "'bedrooms' (integer), 'bathrooms' (integer), 'maxPrice' (number), 'minPrice' (number), " +
                "'propertyType' (e.g., APARTMENT, HOUSE), 'listingPurpose' (RENT, SALE), " +
                "and 'confidence' (float between 0.0 and 1.0). Do not include formatting markup.");

        registerTemplate("intent_parsing", "v2", "Respond in JSON format: Parse query '${query}'. " +
                "Identify fields: intent, city, locality, bedrooms, bathrooms, minPrice, maxPrice, propertyType, listingPurpose, confidence.");

        registerTemplate("assistant_chat", "v1", "You are the RoomWallah Property Assistant. " +
                "Help the user find rent properties in India. " +
                "Adhere to safety rules and do not make up fake info. " +
                "Here is some relevant context from our database:\n${context}");

        // Set active versions
        activeVersions.put("intent_parsing", "v1");
        activeVersions.put("assistant_chat", "v1");
    }

    public void registerTemplate(String templateKey, String version, String templateContent) {
        templates.computeIfAbsent(templateKey, k -> new ConcurrentHashMap<>()).put(version, templateContent);
        log.info("Registered prompt template: key={}, version={}", templateKey, version);
    }

    public String getTemplate(String templateKey) {
        String activeVersion = activeVersions.getOrDefault(templateKey, "v1");
        return getTemplate(templateKey, activeVersion);
    }

    public String getTemplate(String templateKey, String version) {
        Map<String, String> versionMap = templates.get(templateKey);
        if (versionMap == null || !versionMap.containsKey(version)) {
            log.warn("Prompt template version not found: key={}, version={}. Falling back to default.", templateKey, version);
            return versionMap != null ? versionMap.values().iterator().next() : "";
        }
        return versionMap.get(version);
    }

    public String getActiveVersion(String templateKey) {
        return activeVersions.getOrDefault(templateKey, "v1");
    }

    public void rollbackOrSetVersion(String templateKey, String version) {
        Map<String, String> versionMap = templates.get(templateKey);
        if (versionMap == null || !versionMap.containsKey(version)) {
            throw new IllegalArgumentException("Cannot set active version; template key or version does not exist: " + templateKey + ":" + version);
        }
        activeVersions.put(templateKey, version);
        log.info("Switched active version for prompt template '{}' to '{}'", templateKey, version);
    }
}
