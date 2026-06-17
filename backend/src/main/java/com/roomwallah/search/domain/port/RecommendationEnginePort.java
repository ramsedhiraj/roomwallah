package com.roomwallah.search.domain.port;

import com.roomwallah.search.domain.entity.SearchDocument;

import java.util.List;
import java.util.UUID;

public interface RecommendationEnginePort {

    List<RecommendationItem> recommend(UUID userId, int limit);

    record RecommendationItem(
            SearchDocument document,
            List<String> reasons
    ) implements java.io.Serializable {}
}
