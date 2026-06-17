package com.roomwallah.search.domain.port;

import com.roomwallah.search.domain.model.SearchQuery;

public interface SearchEnginePort {

    SearchResult search(SearchQuery query);

    long count(SearchQuery query);

    boolean isAvailable();

    String providerName();

    record SearchResult(
            java.util.List<com.roomwallah.search.domain.entity.SearchDocument> documents,
            String nextCursor,
            long totalCount
    ) {}
}
