package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.entity.SearchAnalytics;
import com.roomwallah.search.domain.port.SearchAnalyticsPort;
import com.roomwallah.search.domain.repository.SearchAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaSearchAnalyticsAdapter implements SearchAnalyticsPort {

    private final SearchAnalyticsRepository searchAnalyticsRepository;

    @Override
    @Transactional
    public void recordSearch(SearchAnalytics analytics) {
        searchAnalyticsRepository.save(analytics);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchAnalytics> findRecent(int limit) {
        // Return recent items ordered by createdAt desc. Let's find using PageRequest.
        return searchAnalyticsRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return searchAnalyticsRepository.countByCreatedAtAfter(since);
    }

    @Override
    @Transactional
    public void deleteOlderThan(Instant cutoff) {
        searchAnalyticsRepository.deleteByCreatedAtBefore(cutoff);
    }
}
