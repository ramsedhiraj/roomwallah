package com.roomwallah.search.domain.port;

import com.roomwallah.search.domain.entity.SearchDocument;

import java.util.List;

public interface RerankingPort {

    List<SearchDocument> rerank(String query, List<SearchDocument> candidates);

    boolean isAvailable();
}
