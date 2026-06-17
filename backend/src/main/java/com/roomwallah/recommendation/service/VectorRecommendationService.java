package com.roomwallah.recommendation.service;

import com.roomwallah.common.ai.EmbeddingProvider;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.recommendation.domain.RecommendationClick;
import com.roomwallah.recommendation.domain.UserVectorPreference;
import com.roomwallah.recommendation.domain.VectorStore;
import com.roomwallah.recommendation.repository.RecommendationClickRepository;
import com.roomwallah.recommendation.repository.UserVectorPreferenceRepository;
import com.roomwallah.search.application.service.SearchExperimentRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorRecommendationService {

    private final UserVectorPreferenceRepository userPreferenceRepository;
    private final RecommendationClickRepository clickRepository;
    private final PropertyRepository propertyRepository;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final SearchExperimentRouter experimentRouter;
    private final AiObservabilityService observabilityService;

    @Transactional
    public void refinePreferences(UUID userId, double budgetLimit, String targetCity, boolean hasGym, boolean hasParking) {
        String preferencesText = String.format("Budget: %s. City: %s. Gym: %s. Parking: %s.",
                budgetLimit, targetCity, hasGym, hasParking);

        log.info("Refining vector preference for user: {} with: {}", userId, preferencesText);
        double[] newEmbedding = embeddingProvider.embed(preferencesText);

        Optional<UserVectorPreference> existing = userPreferenceRepository.findByUserId(userId);
        UserVectorPreference preference;
        if (existing.isPresent()) {
            preference = existing.get();
            preference.setPreferredEmbedding(newEmbedding);
            preference.setLastUpdated(Instant.now());
            preference.setUpdatedAt(Instant.now());
        } else {
            preference = UserVectorPreference.builder()
                    .userId(userId)
                    .preferredEmbedding(newEmbedding)
                    .lastUpdated(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
        userPreferenceRepository.save(preference);
    }

    @Transactional
    public void trackClick(UUID userId, UUID propertyId, String algorithmVersion) {
        RecommendationClick click = RecommendationClick.builder()
                .userId(userId)
                .listingId(propertyId)
                .algorithmVersion(algorithmVersion != null ? algorithmVersion : "RoomGNN-v3-cf")
                .clickedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        clickRepository.save(click);
        observabilityService.trackRecommendationInteraction(true);
        log.info("Tracked recommendation click for user: {}, property: {}, alg: {}", 
                userId, propertyId, algorithmVersion);
    }

    public List<Property> getPersonalizedFeed(UUID userId) {
        observabilityService.trackRecommendationInteraction(false); // log impression
        
        // A/B test routing
        SearchExperimentRouter.ExperimentBucket bucket = experimentRouter.getBucket("personalization", userId, null);
        String algVersion = bucket == SearchExperimentRouter.ExperimentBucket.TREATMENT ? "RoomBERT-v2" : "RoomGNN-v3-cf";
        log.info("Routing user: {} to A/B experiment bucket: {}, algorithm: {}", userId, bucket, algVersion);

        Optional<UserVectorPreference> preferenceOpt = userPreferenceRepository.findByUserId(userId);
        if (preferenceOpt.isEmpty() || preferenceOpt.get().getPreferredEmbedding() == null) {
            log.info("No vector preference found for user: {}. Falling back to default feed.", userId);
            return propertyRepository.findAll().stream().limit(5).toList();
        }

        double[] userVector = preferenceOpt.get().getPreferredEmbedding();
        List<UUID> matchingIds = vectorStore.findSimilarListings(userVector, 5);

        List<Property> feed = new ArrayList<>();
        for (UUID id : matchingIds) {
            propertyRepository.findById(id).ifPresent(feed::add);
        }

        // If not enough vector properties, pad with standard ones
        if (feed.size() < 5) {
            List<Property> remaining = propertyRepository.findAll().stream()
                    .filter(p -> !feed.contains(p))
                    .limit(5 - feed.size())
                    .toList();
            feed.addAll(remaining);
        }

        return feed;
    }
}
