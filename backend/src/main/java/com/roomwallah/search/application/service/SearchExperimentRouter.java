package com.roomwallah.search.application.service;

import com.roomwallah.search.infrastructure.config.SearchFeatureFlags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchExperimentRouter {

    private final SearchFeatureFlags featureFlags;

    public enum ExperimentBucket {
        CONTROL,
        TREATMENT
    }

    public ExperimentBucket getBucket(String featureName, UUID userId, String deviceId) {
        String rolloutConfig = getRolloutConfig(featureName);
        if ("enabled".equalsIgnoreCase(rolloutConfig)) {
            return ExperimentBucket.TREATMENT;
        }
        if ("disabled".equalsIgnoreCase(rolloutConfig) || rolloutConfig == null) {
            return ExperimentBucket.CONTROL;
        }
        if ("admin-only".equalsIgnoreCase(rolloutConfig)) {
            return ExperimentBucket.CONTROL;
        }

        // Percentage split: e.g. "rollout-25" or "25%"
        int percentage = 0;
        try {
            if (rolloutConfig.startsWith("rollout-")) {
                percentage = Integer.parseInt(rolloutConfig.substring(8));
            } else if (rolloutConfig.endsWith("%")) {
                percentage = Integer.parseInt(rolloutConfig.substring(0, rolloutConfig.length() - 1));
            } else {
                percentage = Integer.parseInt(rolloutConfig);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid rollout configuration for feature {}: {}, defaulting to CONTROL", featureName, rolloutConfig);
            return ExperimentBucket.CONTROL;
        }

        String targetId = userId != null ? userId.toString() : (deviceId != null ? deviceId : "");
        if (targetId.isEmpty()) {
            return ExperimentBucket.CONTROL;
        }

        int hashValue = getStableHash(targetId);
        if (hashValue < percentage) {
            return ExperimentBucket.TREATMENT;
        }
        return ExperimentBucket.CONTROL;
    }

    private String getRolloutConfig(String featureName) {
        return switch (featureName.toLowerCase()) {
            case "semanticsearch" -> featureFlags.getSemanticSearch();
            case "aireranking" -> featureFlags.getAiReranking();
            case "personalization" -> featureFlags.getPersonalization();
            case "trendingboosts" -> featureFlags.getTrendingBoosts();
            case "hybridsearch" -> featureFlags.getHybridSearch();
            case "explainranking" -> featureFlags.getExplainRanking();
            default -> "disabled";
        };
    }

    private int getStableHash(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = 31 * hash + input.charAt(i);
        }
        return Math.abs(hash) % 100;
    }
}
