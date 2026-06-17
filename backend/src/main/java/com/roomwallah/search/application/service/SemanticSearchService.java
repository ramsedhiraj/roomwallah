package com.roomwallah.search.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.common.ai.AiProvider;
import com.roomwallah.common.ai.AiSafetyGuard;
import com.roomwallah.search.domain.entity.SearchIntentLog;
import com.roomwallah.search.domain.entity.SearchSynonym;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.repository.SearchIntentLogRepository;
import com.roomwallah.search.domain.repository.SearchSynonymRepository;
import com.roomwallah.common.ai.registry.PromptRegistry;
import com.roomwallah.common.observability.AiObservabilityService;
import com.roomwallah.search.infrastructure.config.SearchFeatureFlags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class SemanticSearchService {

    private final AiProvider aiProvider;
    private final AiSafetyGuard aiSafetyGuard;
    private final SearchSynonymRepository searchSynonymRepository;
    private final SearchIntentLogRepository searchIntentLogRepository;
    private final ObjectMapper objectMapper;
    private final PromptRegistry promptRegistry;
    private final AiObservabilityService observabilityService;
    private final SearchFeatureFlags featureFlags;

    @org.springframework.beans.factory.annotation.Autowired
    public SemanticSearchService(
            AiProvider aiProvider,
            AiSafetyGuard aiSafetyGuard,
            SearchSynonymRepository searchSynonymRepository,
            SearchIntentLogRepository searchIntentLogRepository,
            ObjectMapper objectMapper,
            PromptRegistry promptRegistry,
            AiObservabilityService observabilityService,
            SearchFeatureFlags featureFlags
    ) {
        this.aiProvider = aiProvider;
        this.aiSafetyGuard = aiSafetyGuard;
        this.searchSynonymRepository = searchSynonymRepository;
        this.searchIntentLogRepository = searchIntentLogRepository;
        this.objectMapper = objectMapper;
        this.promptRegistry = promptRegistry;
        this.observabilityService = observabilityService;
        this.featureFlags = featureFlags;
    }

    // Backward-compatible constructor for tests
    public SemanticSearchService(
            AiProvider aiProvider,
            AiSafetyGuard aiSafetyGuard,
            SearchSynonymRepository searchSynonymRepository,
            SearchIntentLogRepository searchIntentLogRepository,
            ObjectMapper objectMapper
    ) {
        this.aiProvider = aiProvider;
        this.aiSafetyGuard = aiSafetyGuard;
        this.searchSynonymRepository = searchSynonymRepository;
        this.searchIntentLogRepository = searchIntentLogRepository;
        this.objectMapper = objectMapper;
        this.promptRegistry = new PromptRegistry();
        this.observabilityService = null;
        this.featureFlags = new SearchFeatureFlags();
        this.featureFlags.setSemanticSearch("enabled");
    }

    public SearchQuery parseAndEnhanceQuery(String userQuery, UUID userId) {
        long startTime = System.currentTimeMillis();
        aiSafetyGuard.checkAuthorization(userId, "SEMANTIC_SEARCH");
        String sanitizedQuery = aiSafetyGuard.validateAndFilterInput(userQuery, userId);

        if (sanitizedQuery == null || sanitizedQuery.trim().isEmpty()) {
            return SearchQuery.builder().text("").build();
        }

        ParsedIntent parsed = null;
        double confidence = 0.0;
        String activeVersion = "v1";
        String modelIdentifier = aiProvider.getModelIdentifier();

        if (!featureFlags.isSemanticSearchEnabled()) {
            log.info("Semantic search feature flag is disabled. Using rule-based fallback.");
            parsed = ruleBasedFallback(sanitizedQuery);
            confidence = 0.4;
        } else {
            try {
                String tenantId = com.roomwallah.security.TenantContext.getCurrentTenant();
                double costEstimate = 0.0001; // Estimate cost per parsing run
                if (observabilityService != null) {
                    observabilityService.verifyTenantQuota(tenantId, costEstimate);
                }

                activeVersion = promptRegistry.getActiveVersion("intent_parsing");
                String template = promptRegistry.getTemplate("intent_parsing", activeVersion);
                String prompt = template.replace("${query}", sanitizedQuery);
                
                long aiStart = System.currentTimeMillis();
                String aiResult = aiProvider.generate(prompt);
                long aiDuration = System.currentTimeMillis() - aiStart;

                if (aiResult.contains("```json")) {
                    aiResult = aiResult.substring(aiResult.indexOf("```json") + 7);
                    aiResult = aiResult.substring(0, aiResult.indexOf("```"));
                } else if (aiResult.contains("```")) {
                    aiResult = aiResult.substring(aiResult.indexOf("```") + 3);
                    aiResult = aiResult.substring(0, aiResult.indexOf("```"));
                }
                aiResult = aiResult.trim();
                parsed = objectMapper.readValue(aiResult, ParsedIntent.class);
                confidence = parsed.getConfidence();

                // Track usage in observability
                if (observabilityService != null) {
                    observabilityService.trackRequest(
                            userId, 
                            tenantId, 
                            modelIdentifier, 
                            prompt.length() / 4, 
                            aiResult.length() / 4, 
                            aiDuration, 
                            true, 
                            null
                    );
                    observabilityService.trackSemanticSearchConfidence(confidence);
                }

            } catch (Exception e) {
                log.warn("Failed to parse intent using AI provider, using fallback: {}", e.getMessage());
            }
        }

        if (parsed == null || confidence < 0.5) {
            log.info("Low confidence intent parse ({}). Falling back to rule-based parser.", confidence);
            parsed = ruleBasedFallback(sanitizedQuery);
            confidence = 0.4;
        }

        String expandedText = expandSynonyms(sanitizedQuery);

        try {
            SearchIntentLog intentLog = SearchIntentLog.builder()
                    .queryText(userQuery)
                    .parsedIntent(objectMapper.writeValueAsString(parsed))
                    .confidence(confidence)
                    .modelVersion(modelIdentifier)
                    .promptTemplateVersion(activeVersion)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            searchIntentLogRepository.save(intentLog);
        } catch (Exception ex) {
            log.error("Failed to log intent search", ex);
        }

        SearchFilter.SearchFilterBuilder filterBuilder = SearchFilter.builder();
        if (parsed.getCity() != null) filterBuilder.city(parsed.getCity());
        if (parsed.getLocality() != null) filterBuilder.locality(parsed.getLocality());
        if (parsed.getPropertyType() != null) filterBuilder.propertyType(parsed.getPropertyType().toUpperCase());
        if (parsed.getListingPurpose() != null) filterBuilder.listingPurpose(parsed.getListingPurpose().toUpperCase());
        if (parsed.getBedrooms() != null) filterBuilder.bedrooms(parsed.getBedrooms());
        if (parsed.getBathrooms() != null) filterBuilder.bathrooms(parsed.getBathrooms());
        
        if (parsed.getMinPrice() != null || parsed.getMaxPrice() != null) {
            filterBuilder.priceRange(new com.roomwallah.search.domain.model.PriceRange(
                    parsed.getMinPrice(), parsed.getMaxPrice()
            ));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Semantic search query processing completed in {} ms. Intent: {}", duration, parsed.getIntent());

        return SearchQuery.builder()
                .text(expandedText)
                .filter(filterBuilder.build())
                .build();
    }

    private String expandSynonyms(String text) {
        String[] tokens = text.split("\\s+");
        List<String> expanded = new ArrayList<>();
        for (String token : tokens) {
            String cleanToken = token.replaceAll("[^a-zA-Z0-9]", "");
            expanded.add(token);
            Optional<SearchSynonym> opt = searchSynonymRepository.findByTermIgnoreCase(cleanToken);
            if (opt.isPresent()) {
                String[] syns = opt.get().getSynonyms().split(",");
                for (String syn : syns) {
                    expanded.add(syn.trim());
                }
            }
        }
        return String.join(" ", expanded);
    }

    private ParsedIntent ruleBasedFallback(String text) {
        ParsedIntent intent = new ParsedIntent();
        intent.setIntent("SEARCH");
        intent.setConfidence(0.4);

        String lower = text.toLowerCase();
        
        if (lower.contains("mumbai")) intent.setCity("Mumbai");
        else if (lower.contains("delhi")) intent.setCity("Delhi");
        else if (lower.contains("bangalore")) intent.setCity("Bangalore");
        
        if (lower.contains("apartment") || lower.contains("flat")) intent.setPropertyType("APARTMENT");
        else if (lower.contains("house") || lower.contains("villa")) intent.setPropertyType("HOUSE");

        if (lower.contains("rent")) intent.setListingPurpose("RENT");
        else if (lower.contains("sale") || lower.contains("buy")) intent.setListingPurpose("SALE");

        if (lower.contains("1 bhk") || lower.contains("1bhk") || lower.contains("1 bedroom")) intent.setBedrooms(1);
        else if (lower.contains("2 bhk") || lower.contains("2bhk") || lower.contains("2 bedroom")) intent.setBedrooms(2);
        else if (lower.contains("3 bhk") || lower.contains("3bhk") || lower.contains("3 bedroom")) intent.setBedrooms(3);

        return intent;
    }

    @lombok.Data
    public static class ParsedIntent {
        private String intent;
        private String city;
        private String locality;
        private Integer bedrooms;
        private Integer bathrooms;
        private java.math.BigDecimal minPrice;
        private java.math.BigDecimal maxPrice;
        private String propertyType;
        private String listingPurpose;
        private double confidence;
    }
}
