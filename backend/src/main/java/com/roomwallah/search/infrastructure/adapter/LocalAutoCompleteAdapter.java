package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.AutoCompletePort;
import com.roomwallah.search.domain.repository.SearchAnalyticsRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.search.domain.repository.TrendingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalAutoCompleteAdapter implements AutoCompletePort {

    private final SearchDocumentRepository searchDocumentRepository;
    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final TrendingQueryRepository trendingQueryRepository;

    @Override
    public List<String> suggest(String prefix, String city, int limit) {
        Set<String> results = new LinkedHashSet<>();

        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyList();
        }

        String prefixLower = prefix.toLowerCase().trim();

        // 1. Matching cities
        try {
            List<String> cities = searchDocumentRepository.findMatchingCities(prefixLower, limit);
            if (cities != null) {
                results.addAll(cities);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch matching cities for autocomplete: {}", e.getMessage());
        }

        // 2. Matching localities
        if (results.size() < limit) {
            try {
                List<String> localities = searchDocumentRepository.findMatchingLocalities(prefixLower, city, limit);
                if (localities != null) {
                    results.addAll(localities);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch matching localities for autocomplete: {}", e.getMessage());
            }
        }

        // 3. Matching property types (fuzzy match against common types)
        if (results.size() < limit) {
            List<String> propertyTypes = List.of("APARTMENT", "FLAT", "PG", "ROOM", "HOUSE", "STUDIO", "VILLA", "INDEPENDENT HOUSE", "PENTHOUSE");
            for (String type : propertyTypes) {
                if (type.toLowerCase().startsWith(prefixLower)) {
                    results.add(type);
                    if (results.size() >= limit) break;
                }
            }
        }

        // 4. Recent searches matching the prefix
        if (results.size() < limit) {
            try {
                var analytics = searchAnalyticsRepository.findTop100ByOrderByCreatedAtDesc();
                for (var a : analytics) {
                    if (a.getSearchText() != null && a.getSearchText().toLowerCase().startsWith(prefixLower)) {
                        results.add(a.getSearchText());
                        if (results.size() >= limit) break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch matching recent searches for autocomplete: {}", e.getMessage());
            }
        }

        // 5. Current trending searches
        if (results.size() < limit) {
            try {
                var trending = (city != null) ?
                        trendingQueryRepository.findByCityOrderBySearchCountDesc(city) :
                        trendingQueryRepository.findTop20ByOrderBySearchCountDesc();
                for (var t : trending) {
                    if (t.getQueryText() != null && t.getQueryText().toLowerCase().startsWith(prefixLower)) {
                        results.add(t.getQueryText());
                        if (results.size() >= limit) break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch matching trending searches for autocomplete: {}", e.getMessage());
            }
        }

        // 6. Titles (general FTS title matches)
        if (results.size() < limit) {
            try {
                List<String> titles = searchDocumentRepository.findAutoCompleteSuggestions(prefixLower, city, limit);
                if (titles != null) {
                    results.addAll(titles);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch matching titles for autocomplete: {}", e.getMessage());
            }
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }
}
