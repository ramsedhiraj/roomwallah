package com.roomwallah.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyType;
import com.roomwallah.property.domain.entity.ListingPurpose;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.GeoLocation;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.search.application.facade.SearchFacade;
import com.roomwallah.search.application.service.SearchIndexService;
import com.roomwallah.search.domain.entity.SavedSearch;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;
import com.roomwallah.search.domain.repository.SavedSearchRepository;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchIntegrationTest {

    @Autowired
    private SearchFacade searchFacade;

    @Autowired
    private SearchIndexService searchIndexService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private SearchDocumentRepository searchDocumentRepository;

    @Autowired
    private SavedSearchRepository savedSearchRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private UUID ownerId;
    private Property activeProperty;

    @BeforeEach
    public void setUp() {
        searchDocumentRepository.deleteAll();
        savedSearchRepository.deleteAll();

        // Clear Redis cache to avoid stale test state
        try {
            java.util.Set<String> keys = redisTemplate.keys("search:results:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            System.err.println("Failed to clear Redis search cache: " + e.getMessage());
        }

        ownerId = UUID.randomUUID();

        // 1. Create a dummy active property in the database
        activeProperty = new Property();
        activeProperty.setOwnerId(ownerId);
        activeProperty.setTitle("Luxurious 2 BHK Apartment in Bandra");
        activeProperty.setDescription("Spacious apartment with amenities.");
        activeProperty.setPropertyType(PropertyType.APARTMENT);
        activeProperty.setListingPurpose(ListingPurpose.RENT);
        activeProperty.setStatus(PropertyStatus.ACTIVE);
        activeProperty.setPrice(new Money(BigDecimal.valueOf(50000), "INR"));
        activeProperty.setAddress(new Address("Line 1", "Bandra West", "Mumbai", "Maharashtra", "India", "400050"));
        activeProperty.setGeoLocation(new GeoLocation(BigDecimal.valueOf(19.0544), BigDecimal.valueOf(72.8402)));
        activeProperty.setBedrooms(2);
        activeProperty.setBathrooms(2);
        activeProperty.setParkingCount(1);
        activeProperty.setPetFriendly(true);
        activeProperty.setSlug("luxurious-2-bhk-apartment-in-bandra-" + System.currentTimeMillis());
        activeProperty.setCreatedAt(Instant.now());
        activeProperty.setUpdatedAt(Instant.now());
        activeProperty.setPublishedAt(Instant.now());
        activeProperty.generateListingRef("Mumbai");

        activeProperty = propertyRepository.save(activeProperty);
    }

    @Test
    public void testIndexingAndPropertySearch() {
        // Index the property
        searchIndexService.indexProperty(activeProperty.getId());

        // Verify it was added to the search index repository
        searchDocumentRepository.flush();

        List<SearchDocument> docs = searchDocumentRepository.findByCity("Mumbai");
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getTitle()).isEqualTo(activeProperty.getTitle());

        // Perform a search query
        SearchFilter filter = SearchFilter.builder()
                .city("Mumbai")
                .bedrooms(2)
                .build();

        SearchQuery query = SearchQuery.builder()
                .text("Bandra")
                .filter(filter)
                .build();

        SearchResult searchResult = searchFacade.search(query, UUID.randomUUID(), "test-corr-id");

        assertThat(searchResult.documents()).hasSize(1);
        assertThat(searchResult.totalCount()).isEqualTo(1);
        assertThat(searchResult.documents().get(0).getPropertyId()).isEqualTo(activeProperty.getId());
    }

    @Test
    public void testAutocompleteSuggestions() {
        searchIndexService.indexProperty(activeProperty.getId());

        List<String> suggestions = searchFacade.autoComplete("Lux", "Mumbai", 5);
        assertThat(suggestions).contains("Luxurious 2 BHK Apartment in Bandra");
    }

    @Test
    public void testSavedSearchConstraints() {
        UUID userId = UUID.randomUUID();
        String queryJson = "{\"text\":\"Bandra\",\"filter\":{\"city\":\"Mumbai\"}}";

        // Save search
        SavedSearch saved = searchFacade.createSavedSearch(userId, queryJson, true);
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.isNotificationEnabled()).isTrue();

        List<SavedSearch> list = searchFacade.getSavedSearches(userId);
        assertThat(list).hasSize(1);

        // Try exceeding max limit of 50 saved searches
        for (int i = 0; i < 49; i++) {
            searchFacade.createSavedSearch(userId, queryJson, false);
        }

        assertThrows(IllegalStateException.class, () -> {
            searchFacade.createSavedSearch(userId, queryJson, false);
        });
    }

    @Test
    public void testIndexDriftReconciliation() {
        // Index active property first
        searchIndexService.indexProperty(activeProperty.getId());
        assertThat(searchDocumentRepository.count()).isEqualTo(1);

        // Manually corrupt index by deleting the document but property still active in db
        searchDocumentRepository.deleteAll();
        assertThat(searchDocumentRepository.count()).isEqualTo(0);

        // Trigger reconciliation
        long repaired = searchFacade.reconcile();
        assertThat(repaired).isEqualTo(1);

        // Verify it was re-indexed and restored
        assertThat(searchDocumentRepository.count()).isEqualTo(1);
    }
}
