package com.roomwallah;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.common.ai.impl.DefaultAiProvider;
import com.roomwallah.common.ai.impl.DefaultChatProvider;
import com.roomwallah.common.ai.impl.DefaultEmbeddingProvider;
import com.roomwallah.common.ai.AiSafetyGuard;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.common.outbox.DomainOutboxEvent;
import com.roomwallah.common.outbox.DomainOutboxEventRepository;
import com.roomwallah.common.outbox.OutboxPublisherJob;
import com.roomwallah.property.application.service.DuplicateListingDetectorImpl;
import com.roomwallah.property.application.service.ListingHealthScoreServiceImpl;
import com.roomwallah.property.application.service.SmartPricingService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.repository.SuspectedDuplicateClusterRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.GeoLocation;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.recommendation.service.DatabaseVectorStore;
import com.roomwallah.search.application.service.SemanticSearchService;
import com.roomwallah.search.domain.entity.SearchSynonym;
import com.roomwallah.search.domain.repository.SearchIntentLogRepository;
import com.roomwallah.search.domain.repository.SearchSynonymRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AiFeaturesTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private SearchSynonymRepository synonymRepository;
    @Mock
    private SearchIntentLogRepository intentLogRepository;
    @Mock
    private SuspectedDuplicateClusterRepository clusterRepository;
    @Mock
    private DomainOutboxEventRepository outboxRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SemanticSearchService semanticSearchService;
    private DuplicateListingDetectorImpl duplicateListingDetector;
    private ListingHealthScoreServiceImpl healthScoreService;
    private SmartPricingService smartPricingService;
    private OutboxPublisherJob outboxPublisherJob;
    
    private final DefaultAiProvider aiProvider = new DefaultAiProvider();
    private final DefaultEmbeddingProvider embeddingProvider = new DefaultEmbeddingProvider();
    private final DefaultChatProvider chatProvider = new DefaultChatProvider();
    private final AiSafetyGuard aiSafetyGuard = new AiSafetyGuard();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        semanticSearchService = new SemanticSearchService(
                aiProvider, 
                aiSafetyGuard, 
                synonymRepository, 
                intentLogRepository, 
                objectMapper
        );

        duplicateListingDetector = new DuplicateListingDetectorImpl(
                propertyRepository, 
                clusterRepository
        );

        healthScoreService = new ListingHealthScoreServiceImpl(propertyRepository);
        smartPricingService = new SmartPricingService(propertyRepository);

        outboxPublisherJob = new OutboxPublisherJob(
                outboxRepository,
                eventPublisher,
                objectMapper
        );
    }

    @Test
    public void testSemanticSearchRuleBasedFallback() {
        // Query that mentions Mumbai and BHK
        String query = "Find 2 BHK flat in Mumbai under 40k";
        when(synonymRepository.findByTermIgnoreCase(anyString())).thenReturn(Optional.empty());

        var result = semanticSearchService.parseAndEnhanceQuery(query, UUID.randomUUID());

        assertNotNull(result);
        assertEquals("Mumbai", result.getFilter().getCity());
        assertEquals(2, result.getFilter().getBedrooms());
        assertEquals("APARTMENT", result.getFilter().getPropertyType());
    }

    @Test
    public void testSemanticSearchSynonymExpansion() {
        String query = "room near station";
        SearchSynonym synonym = SearchSynonym.builder()
                .term("room")
                .synonyms("flat,apartment,bhk")
                .build();

        when(synonymRepository.findByTermIgnoreCase("room")).thenReturn(Optional.of(synonym));
        when(synonymRepository.findByTermIgnoreCase("near")).thenReturn(Optional.empty());
        when(synonymRepository.findByTermIgnoreCase("station")).thenReturn(Optional.empty());

        var result = semanticSearchService.parseAndEnhanceQuery(query, UUID.randomUUID());

        assertNotNull(result);
        assertTrue(result.getText().contains("flat"));
        assertTrue(result.getText().contains("apartment"));
        assertTrue(result.getText().contains("bhk"));
    }

    @Test
    public void testListingHealthScoreCalculations() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Cozy Room in Center Noida");
        property.setDescription("This is a beautiful cozy room near Metro. High-speed internet is available.");
        property.setAddress(new Address("Line 1", "Line 2", "Noida", "UP", "India", "201301"));
        property.setPrice(new Money(BigDecimal.valueOf(12000.0), "INR"));
        property.setBedrooms(1);
        property.setBathrooms(1);
        property.setAmenities(Set.of("Wifi", "Gym", "AC"));

        var report = healthScoreService.calculateHealthScore(property);

        assertNotNull(report);
        assertTrue((int) report.get("completenessScore") > 60);
        assertTrue((boolean) report.get("isHealthy"));
    }

    @Test
    public void testDuplicateDetectionSimilarityScore() {
        Property p1 = new Property();
        p1.setId(UUID.randomUUID());
        p1.setTitle("2 BHK Noida Sector 62");
        p1.setDescription("Spacious 2 BHK near corporate parks and metro station.");
        p1.setGeoLocation(new GeoLocation(BigDecimal.valueOf(28.62), BigDecimal.valueOf(77.36)));
        p1.setPrice(new Money(BigDecimal.valueOf(20000.0), "INR"));

        Property p2 = new Property();
        p2.setId(UUID.randomUUID());
        p2.setTitle("2 BHK flat in Sector 62 Noida");
        p2.setDescription("Spacious 2 BHK near corporate parks and metro station.");
        p2.setGeoLocation(new GeoLocation(BigDecimal.valueOf(28.6205), BigDecimal.valueOf(77.3605)));
        p2.setPrice(new Money(BigDecimal.valueOf(20500.0), "INR"));

        double similarity = duplicateListingDetector.calculateSimilarityScore(p1, p2);
        assertTrue(similarity > 0.75, "Listings should have high similarity score");
    }

    @Test
    public void testOutboxPublisherDLQTransition() {
        DomainOutboxEvent failedEvent = DomainOutboxEvent.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("test-key")
                .aggregateType("Property")
                .aggregateId("test-id")
                .eventType("PROPERTY_PUBLISHED_SIMULATED_FAIL")
                .payload("{}")
                .status("PENDING")
                .retryCount(4)
                .nextAttemptAt(Instant.now().minusSeconds(10))
                .createdAt(Instant.now())
                .build();

        when(outboxRepository.findPendingEvents(any())).thenReturn(List.of(failedEvent));

        outboxPublisherJob.processOutbox();

        verify(outboxRepository, times(1)).save(argThat(event -> 
                "DLQ".equals(event.getStatus()) && event.getRetryCount() == 5
        ));
    }
}
