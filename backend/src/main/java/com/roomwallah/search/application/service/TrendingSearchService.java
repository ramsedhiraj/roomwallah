package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.entity.TrendingQuery;

import java.util.List;

public interface TrendingSearchService {
    void recordSearchQuery(String queryText, String city);
    List<TrendingQuery> getTrending(String city, int limit);
    void aggregateTrendingQueries();
}
