package com.roomwallah.search.domain.port;

public interface SearchRankingPolicyPort {

    double getTrustScoreWeight();

    double getFreshnessWeight();

    double getMediaCompletenessWeight();

    double getVerificationWeight();

    double getRelevanceWeight();
}
