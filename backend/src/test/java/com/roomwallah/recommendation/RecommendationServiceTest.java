package com.roomwallah.recommendation;

import com.roomwallah.common.cache.MultiLevelCacheService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.recommendation.domain.ListingInteraction;
import com.roomwallah.recommendation.domain.RecommendationWeight;
import com.roomwallah.recommendation.dto.RecommendationResponse;
import com.roomwallah.recommendation.repository.ListingInteractionRepository;
import com.roomwallah.recommendation.repository.RecommendationWeightRepository;
import com.roomwallah.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

public class RecommendationServiceTest {

    @Mock
    private ListingInteractionRepository listingInteractionRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private RecommendationWeightRepository recommendationWeightRepository;

    @Mock
    private MultiLevelCacheService multiLevelCacheService;

    private RecommendationService recommendationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        recommendationService = new RecommendationService(
                listingInteractionRepository, propertyRepository,
                recommendationWeightRepository, multiLevelCacheService
        );
    }

    @Test
    public void testLogInteraction_CacheEvicted() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        recommendationService.logInteraction(userId, listingId, "VIEW");

        verify(listingInteractionRepository, times(1)).save(any(ListingInteraction.class));
        verify(multiLevelCacheService, times(1)).evict("recommendations", "similar:" + listingId);
        verify(multiLevelCacheService, times(1)).evict("recommendations", "trending:5");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetSimilarListingsWithExplanation() {
        UUID sourceId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        Property source = new Property();
        source.setId(sourceId);
        source.setAddress(new Address("L1", "L2", "Delhi", "Delhi", "India", "110001"));
        source.setPrice(new Money(BigDecimal.valueOf(10000.00), "INR"));

        Property candidate = new Property();
        candidate.setId(candidateId);
        candidate.setStatus(PropertyStatus.ACTIVE);
        candidate.setAddress(new Address("L1", "L2", "Delhi", "Delhi", "India", "110001"));
        candidate.setPrice(new Money(BigDecimal.valueOf(9800.00), "INR"));
        candidate.setPublishedAt(Instant.now());

        RecommendationWeight weight = RecommendationWeight.builder()
                .budgetWeight(0.3)
                .proximityWeight(0.4)
                .recencyWeight(0.15)
                .popularityWeight(0.15)
                .build();

        when(propertyRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(propertyRepository.findAll()).thenReturn(List.of(source, candidate));
        when(recommendationWeightRepository.findAll()).thenReturn(List.of(weight));

        // Stub cache read for weights
        when(multiLevelCacheService.get(eq("recommendations"), eq("scoring_weights"), eq(RecommendationWeight.class), any()))
                .thenAnswer(inv -> {
                    Supplier<RecommendationWeight> s = inv.getArgument(3);
                    return s.get();
                });

        List<RecommendationResponse> results = recommendationService.getSimilarListingsWithExplanation(sourceId);

        assertFalse(results.isEmpty());
        assertEquals(candidateId, results.get(0).getProperty().getId());
    }
}
