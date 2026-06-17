package com.roomwallah.search;

import com.roomwallah.search.application.service.SearchExperimentRouter;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;
import com.roomwallah.search.infrastructure.config.SearchFeatureFlags;
import com.roomwallah.search.presentation.controller.SearchController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchEnhancementsTest {

    @Autowired
    private SearchExperimentRouter experimentRouter;

    @Autowired
    private SearchFeatureFlags featureFlags;

    @Autowired
    private SearchController searchController;

    @Test
    public void testExperimentRouterStability() {
        UUID userId = UUID.randomUUID();
        String deviceId = "device-123456";

        featureFlags.setHybridSearch("rollout-25");
        
        SearchExperimentRouter.ExperimentBucket bucket1 = experimentRouter.getBucket("hybridSearch", userId, deviceId);
        SearchExperimentRouter.ExperimentBucket bucket2 = experimentRouter.getBucket("hybridSearch", userId, deviceId);

        assertThat(bucket1).isEqualTo(bucket2);
    }

    @Test
    public void testExperimentPercentageDistribution() {
        featureFlags.setHybridSearch("rollout-50");

        int treatmentCount = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            UUID id = UUID.randomUUID();
            if (experimentRouter.getBucket("hybridSearch", id, null) == SearchExperimentRouter.ExperimentBucket.TREATMENT) {
                treatmentCount++;
            }
        }

        assertThat(treatmentCount).isBetween(350, 650);
    }

    @Test
    public void testQueryConstraintsValidation() {
        String longText = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> {
            searchController.search(longText, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("Bandra", null, null, null, null, null, null, null, null, null, null, 19.0544, 72.8402, 51.0, null, null, null, null, null, null, null, null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            searchController.search("%%%", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        });
    }
}
