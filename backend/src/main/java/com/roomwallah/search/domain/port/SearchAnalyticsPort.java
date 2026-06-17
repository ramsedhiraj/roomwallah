package com.roomwallah.search.domain.port;

import com.roomwallah.search.domain.entity.SearchAnalytics;

import java.time.Instant;
import java.util.List;

public interface SearchAnalyticsPort {

    void recordSearch(SearchAnalytics analytics);

    List<SearchAnalytics> findRecent(int limit);

    long countSince(Instant since);

    void deleteOlderThan(Instant cutoff);
}
