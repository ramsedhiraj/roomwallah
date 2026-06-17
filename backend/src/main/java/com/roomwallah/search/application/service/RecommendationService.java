package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.port.RecommendationEnginePort.RecommendationItem;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
    List<RecommendationItem> getRecommendations(UUID userId, int limit);
    void refreshRecommendationsCache();
}
