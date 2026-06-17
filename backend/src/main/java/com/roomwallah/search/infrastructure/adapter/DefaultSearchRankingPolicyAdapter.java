package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.SearchRankingPolicyPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultSearchRankingPolicyAdapter implements SearchRankingPolicyPort {

    @Value("${roomwallah.search.ranking.trust-score-weight:0.2}")
    private double trustScoreWeight;

    @Value("${roomwallah.search.ranking.freshness-weight:0.25}")
    private double freshnessWeight;

    @Value("${roomwallah.search.ranking.media-completeness-weight:0.15}")
    private double mediaCompletenessWeight;

    @Value("${roomwallah.search.ranking.verification-weight:0.2}")
    private double verificationWeight;

    @Value("${roomwallah.search.ranking.relevance-weight:0.2}")
    private double relevanceWeight;

    @Override
    public double getTrustScoreWeight() {
        return trustScoreWeight;
    }

    @Override
    public double getFreshnessWeight() {
        return freshnessWeight;
    }

    @Override
    public double getMediaCompletenessWeight() {
        return mediaCompletenessWeight;
    }

    @Override
    public double getVerificationWeight() {
        return verificationWeight;
    }

    @Override
    public double getRelevanceWeight() {
        return relevanceWeight;
    }
}
