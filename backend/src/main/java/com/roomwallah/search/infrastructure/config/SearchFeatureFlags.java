package com.roomwallah.search.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "roomwallah.search.features")
@Getter
@Setter
public class SearchFeatureFlags {
    private String semanticSearch = "disabled";
    private String aiReranking = "disabled";
    private String personalization = "disabled";
    private String trendingBoosts = "disabled";
    private String hybridSearch = "disabled";
    private String explainRanking = "disabled";
    private String assistantChat = "enabled";

    public boolean isSemanticSearchEnabled() {
        return "enabled".equalsIgnoreCase(semanticSearch);
    }

    public boolean isAiRerankingEnabled() {
        return "enabled".equalsIgnoreCase(aiReranking);
    }

    public boolean isPersonalizationEnabled() {
        return "enabled".equalsIgnoreCase(personalization);
    }

    public boolean isTrendingBoostsEnabled() {
        return "enabled".equalsIgnoreCase(trendingBoosts);
    }

    public boolean isHybridSearchEnabled() {
        return "enabled".equalsIgnoreCase(hybridSearch);
    }

    public boolean isExplainRankingEnabled() {
        return "enabled".equalsIgnoreCase(explainRanking);
    }

    public boolean isAssistantChatEnabled() {
        return "enabled".equalsIgnoreCase(assistantChat);
    }
}
