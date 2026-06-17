package com.roomwallah.search.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.search.domain.entity.SearchAnalytics;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.port.RecommendationEnginePort;
import com.roomwallah.search.domain.repository.SearchAnalyticsRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.search.infrastructure.config.SearchFeatureFlags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleRecommendationAdapter implements RecommendationEnginePort {

    private final SearchDocumentRepository searchDocumentRepository;
    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final ObjectMapper objectMapper;
    private final SearchFeatureFlags featureFlags;

    @Override
    public List<RecommendationItem> recommend(UUID userId, int limit) {
        if (!featureFlags.isPersonalizationEnabled() || userId == null) {
            return getDefaultRecommendations(limit);
        }

        List<SearchAnalytics> userSearches = searchAnalyticsRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userSearches.isEmpty()) {
            return getDefaultRecommendations(limit);
        }

        // Try to identify preferred city and price ranges
        String preferredCity = null;
        BigDecimal avgMinPrice = null;
        BigDecimal avgMaxPrice = null;
        int priceCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (SearchAnalytics analytics : userSearches) {
            String json = analytics.getFiltersJson();
            if (json != null && !json.isBlank()) {
                try {
                    Map<?, ?> filters = objectMapper.readValue(json, Map.class);
                    if (preferredCity == null && filters.get("city") != null) {
                        preferredCity = filters.get("city").toString();
                    }
                    if (filters.get("priceRange") instanceof Map) {
                        Map<?, ?> priceRange = (Map<?, ?>) filters.get("priceRange");
                        if (priceRange.get("minPrice") != null) {
                            totalPrice = totalPrice.add(new BigDecimal(priceRange.get("minPrice").toString()));
                            priceCount++;
                        }
                        if (priceRange.get("maxPrice") != null) {
                            totalPrice = totalPrice.add(new BigDecimal(priceRange.get("maxPrice").toString()));
                            priceCount++;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse filter JSON: {}", json, e);
                }
            }
        }

        List<SearchDocument> candidates;
        if (preferredCity != null) {
            candidates = searchDocumentRepository.findByCity(preferredCity);
        } else {
            candidates = searchDocumentRepository.findByPropertyStatus("ACTIVE");
        }

        if (candidates.isEmpty()) {
            candidates = searchDocumentRepository.findByPropertyStatus("ACTIVE");
        }

        final BigDecimal averageSearchPrice = priceCount > 0 ? 
                totalPrice.divide(BigDecimal.valueOf(priceCount), 2, RoundingMode.HALF_UP) : null;

        List<ScoredItem> scoredItems = candidates.stream()
                .filter(doc -> "ACTIVE".equalsIgnoreCase(doc.getPropertyStatus()))
                .map(doc -> {
                    List<String> reasons = new ArrayList<>();
                    int score = 0;
                    if (doc.isOwnerVerified()) {
                        reasons.add("Verified Owner");
                        score += 3;
                    }
                    if (doc.getTrustScore() > 80) {
                        reasons.add("Highly Trusted Listing");
                        score += 2;
                    }
                    if (averageSearchPrice != null) {
                        BigDecimal docPrice = doc.getPrice();
                        BigDecimal diff = docPrice.subtract(averageSearchPrice).abs();
                        BigDecimal threshold = averageSearchPrice.multiply(new BigDecimal("0.3")); // 30% difference
                        if (diff.compareTo(threshold) <= 0) {
                            reasons.add("Within Your Typical Budget");
                            score += 4;
                        }
                    }
                    if (reasons.isEmpty()) {
                        reasons.add("Popular Listing");
                    }
                    return new ScoredItem(doc, reasons, score);
                })
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .collect(Collectors.toList());

        return diversifyRecommendations(scoredItems, limit);
    }

    private List<RecommendationItem> getDefaultRecommendations(int limit) {
        List<SearchDocument> active = searchDocumentRepository.findByPropertyStatus("ACTIVE");
        List<ScoredItem> scoredItems = active.stream()
                .map(doc -> {
                    List<String> reasons = new ArrayList<>();
                    int score = 0;
                    if (doc.isOwnerVerified()) {
                        reasons.add("Verified Owner");
                        score += 3;
                    }
                    if (doc.getTrustScore() > 80) {
                        reasons.add("Highly Trusted Listing");
                        score += 2;
                    }
                    if (reasons.isEmpty()) {
                        reasons.add("Freshly Listed");
                    }
                    return new ScoredItem(doc, reasons, score);
                })
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .collect(Collectors.toList());

        return diversifyRecommendations(scoredItems, limit);
    }

    private List<RecommendationItem> diversifyRecommendations(List<ScoredItem> scoredItems, int limit) {
        if (scoredItems.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<ScoredItem>> grouped = scoredItems.stream()
                .collect(Collectors.groupingBy(item -> 
                    (item.document().getPropertyType() != null ? item.document().getPropertyType() : "UnknownType") + ":" + 
                    (item.document().getLocality() != null ? item.document().getLocality() : "UnknownLocality")
                ));

        List<List<ScoredItem>> lists = new ArrayList<>(grouped.values());
        for (List<ScoredItem> group : lists) {
            group.sort(Comparator.comparingInt(ScoredItem::score).reversed());
        }
        lists.sort((g1, g2) -> Integer.compare(g2.get(0).score(), g1.get(0).score()));

        List<ScoredItem> diverseList = new ArrayList<>();
        int index = 0;
        boolean addedAny = true;
        while (diverseList.size() < limit && addedAny) {
            addedAny = false;
            for (List<ScoredItem> group : lists) {
                if (index < group.size()) {
                    diverseList.add(group.get(index));
                    addedAny = true;
                    if (diverseList.size() == limit) {
                        break;
                    }
                }
            }
            index++;
        }

        return diverseList.stream()
                .map(item -> new RecommendationItem(item.document(), item.reasons()))
                .collect(Collectors.toList());
    }

    private record ScoredItem(SearchDocument document, List<String> reasons, int score) {}
}
