package com.roomwallah.recommendation.service;

import com.roomwallah.common.cache.MultiLevelCacheService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.recommendation.domain.ListingInteraction;
import com.roomwallah.recommendation.domain.RecommendationWeight;
import com.roomwallah.recommendation.dto.RecommendationResponse;
import com.roomwallah.recommendation.dto.ScoringExplanation;
import com.roomwallah.recommendation.repository.ListingInteractionRepository;
import com.roomwallah.recommendation.repository.RecommendationWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ListingInteractionRepository listingInteractionRepository;
    private final PropertyRepository propertyRepository;
    private final RecommendationWeightRepository recommendationWeightRepository;
    private final MultiLevelCacheService multiLevelCacheService;

    @Transactional
    public void logInteraction(UUID userId, UUID listingId, String interactionType) {
        ListingInteraction interaction = ListingInteraction.builder()
                .userId(userId)
                .listingId(listingId)
                .interactionType(interactionType != null ? interactionType.toUpperCase() : "VIEW")
                .interactionTime(Instant.now())
                .build();
        listingInteractionRepository.save(interaction);
        log.debug("Logged interaction for listing: {}, type: {}", listingId, interactionType);
        
        multiLevelCacheService.evict("recommendations", "trending:5");
        multiLevelCacheService.evict("recommendations", "trending:10");
        multiLevelCacheService.evict("recommendations", "similar:" + listingId);
    }

    @SuppressWarnings("unchecked")
    public List<RecommendationResponse> getSimilarListingsCached(UUID listingId) {
        return multiLevelCacheService.get("recommendations", "similar:" + listingId, List.class, () -> getSimilarListingsWithExplanation(listingId));
    }

    @SuppressWarnings("unchecked")
    public List<Property> getTrendingListingsCached(int limit) {
        return multiLevelCacheService.get("recommendations", "trending:" + limit, List.class, () -> getTrendingListings(limit));
    }

    @Transactional(readOnly = true)
    public RecommendationWeight getWeights() {
        return multiLevelCacheService.get("recommendations", "scoring_weights", RecommendationWeight.class, () -> {
            List<RecommendationWeight> list = recommendationWeightRepository.findAll();
            if (list.isEmpty()) {
                RecommendationWeight defaultWeight = RecommendationWeight.builder()
                        .budgetWeight(0.3)
                        .proximityWeight(0.4)
                        .recencyWeight(0.15)
                        .popularityWeight(0.15)
                        .build();
                defaultWeight.setVersion(0L);
                return recommendationWeightRepository.save(defaultWeight);
            }
            return list.get(0);
        });
    }

    @Transactional
    public RecommendationWeight updateWeights(double budget, double proximity, double recency, double popularity) {
        RecommendationWeight weight = getWeights();
        weight.setBudgetWeight(budget);
        weight.setProximityWeight(proximity);
        weight.setRecencyWeight(recency);
        weight.setPopularityWeight(popularity);
        RecommendationWeight saved = recommendationWeightRepository.save(weight);
        multiLevelCacheService.evict("recommendations", "scoring_weights");
        log.info("Updated recommendation weights. Budget: {}, Proximity: {}, Recency: {}, Popularity: {}", budget, proximity, recency, popularity);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getSimilarListingsWithExplanation(UUID listingId) {
        Property source = propertyRepository.findById(listingId).orElse(null);
        if (source == null || source.getAddress() == null || source.getPrice() == null) {
            return Collections.emptyList();
        }

        String city = source.getAddress().getCity();
        String state = source.getAddress().getState();
        BigDecimal sourcePrice = source.getPrice().getAmount();

        RecommendationWeight weights = getWeights();

        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> topInteractions = listingInteractionRepository.findTopInteractions(since);
        Map<UUID, Long> popularityMap = new HashMap<>();
        for (Object[] row : topInteractions) {
            popularityMap.put((UUID) row[0], (Long) row[1]);
        }

        List<Property> candidates = propertyRepository.findAll().stream()
                .filter(p -> !p.getId().equals(listingId))
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .toList();

        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Property candidate : candidates) {
            double rawProximity = 0;
            double rawBudget = 0;
            double rawRecency = 0;
            double rawPopularity = 0;

            if (candidate.getAddress() != null) {
                if (city.equalsIgnoreCase(candidate.getAddress().getCity())) {
                    rawProximity = 100.0;
                } else if (state.equalsIgnoreCase(candidate.getAddress().getState())) {
                    rawProximity = 40.0;
                }
            }

            if (candidate.getPrice() != null && candidate.getPrice().getAmount() != null && sourcePrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal candidatePrice = candidate.getPrice().getAmount();
                double diffRatio = candidatePrice.subtract(sourcePrice).abs().doubleValue() / sourcePrice.doubleValue();
                if (diffRatio <= 0.05) {
                    rawBudget = 100.0;
                } else if (diffRatio <= 0.10) {
                    rawBudget = 70.0;
                } else if (diffRatio <= 0.20) {
                    rawBudget = 40.0;
                }
            }

            Instant publishedAt = candidate.getPublishedAt();
            if (publishedAt != null) {
                long daysOld = ChronoUnit.DAYS.between(publishedAt, Instant.now());
                if (daysOld <= 7) {
                    rawRecency = 100.0;
                } else if (daysOld <= 30) {
                    rawRecency = 70.0;
                } else if (daysOld <= 90) {
                    rawRecency = 40.0;
                }
            }

            Long interactionCount = popularityMap.getOrDefault(candidate.getId(), 0L);
            rawPopularity = Math.min(interactionCount * 10.0, 100.0);

            double proximityScore = rawProximity * weights.getProximityWeight();
            double budgetAffinityScore = rawBudget * weights.getBudgetWeight();
            double recencyScore = rawRecency * weights.getRecencyWeight();
            double popularityScore = rawPopularity * weights.getPopularityWeight();

            double totalScore = proximityScore + budgetAffinityScore + recencyScore + popularityScore;
            String details = String.format("Proximity: %.1f (w=%.2f), Budget affinity: %.1f (w=%.2f), Recency: %.1f (w=%.2f), Popularity: %.1f (w=%.2f)",
                    proximityScore, weights.getProximityWeight(),
                    budgetAffinityScore, weights.getBudgetWeight(),
                    recencyScore, weights.getRecencyWeight(),
                    popularityScore, weights.getPopularityWeight());

            ScoringExplanation explanation = ScoringExplanation.builder()
                    .proximityScore(proximityScore)
                    .budgetAffinityScore(budgetAffinityScore)
                    .recencyScore(recencyScore)
                    .popularityScore(popularityScore)
                    .totalScore(totalScore)
                    .explanationDetails(details)
                    .build();

            recommendations.add(RecommendationResponse.builder()
                    .property(candidate)
                    .explanation(explanation)
                    .build());
        }

        recommendations.sort((r1, r2) -> Double.compare(r2.getExplanation().getTotalScore(), r1.getExplanation().getTotalScore()));
        return recommendations;
    }

    @Transactional(readOnly = true)
    public List<Property> getTrendingListings(int limit) {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> topInteractions = listingInteractionRepository.findTopInteractions(since);

        List<UUID> trendingIds = topInteractions.stream()
                .map(row -> (UUID) row[0])
                .limit(limit)
                .toList();

        List<Property> trending = new ArrayList<>();
        for (UUID id : trendingIds) {
            propertyRepository.findById(id)
                    .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                    .ifPresent(trending::add);
        }

        if (trending.size() < limit) {
            List<Property> active = propertyRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                    .filter(p -> !trending.contains(p))
                    .limit(limit - trending.size())
                    .toList();
            trending.addAll(active);
        }

        return trending;
    }
}
