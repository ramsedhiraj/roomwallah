package com.roomwallah.common.ai.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.search.application.service.SemanticSearchService;
import com.roomwallah.search.domain.model.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBenchmarkService {

    private final SemanticSearchService semanticSearchService;
    private final ObjectMapper objectMapper;

    public static class BenchmarkTestCase {
        public String query;
        public String expectedCity;
        public Integer expectedBedrooms;
        public String expectedListingPurpose;

        public BenchmarkTestCase(String query, String expectedCity, Integer expectedBedrooms, String expectedListingPurpose) {
            this.query = query;
            this.expectedCity = expectedCity;
            this.expectedBedrooms = expectedBedrooms;
            this.expectedListingPurpose = expectedListingPurpose;
        }
    }

    public Map<String, Object> runEvaluations() {
        List<BenchmarkTestCase> testCases = List.of(
                new BenchmarkTestCase("2 bhk in Mumbai under 50k", "Mumbai", 2, null),
                new BenchmarkTestCase("1 bhk flat Delhi rent below 15000", "Delhi", 1, "RENT"),
                new BenchmarkTestCase("room for rent in Bangalore near tech park", "Bangalore", null, "RENT")
        );

        int runs = 0;
        int passed = 0;
        long totalLatencyMs = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        UUID testUserId = UUID.randomUUID();

        for (BenchmarkTestCase tc : testCases) {
            runs++;
            long start = System.currentTimeMillis();
            boolean success = false;
            String errorMsg = "";

            try {
                SearchQuery parsed = semanticSearchService.parseAndEnhanceQuery(tc.query, testUserId);
                
                boolean cityMatch = tc.expectedCity == null || tc.expectedCity.equalsIgnoreCase(parsed.getFilter().getCity());
                boolean bedMatch = tc.expectedBedrooms == null || tc.expectedBedrooms.equals(parsed.getFilter().getBedrooms());
                boolean purposeMatch = tc.expectedListingPurpose == null || tc.expectedListingPurpose.equalsIgnoreCase(parsed.getFilter().getListingPurpose());

                if (cityMatch && bedMatch && purposeMatch) {
                    passed++;
                    success = true;
                } else {
                    errorMsg = String.format("Mismatch. Expected: City=%s, Beds=%s, Purpose=%s. Got: City=%s, Beds=%s, Purpose=%s",
                            tc.expectedCity, tc.expectedBedrooms, tc.expectedListingPurpose,
                            parsed.getFilter().getCity(), parsed.getFilter().getBedrooms(), parsed.getFilter().getListingPurpose());
                }
            } catch (Exception e) {
                errorMsg = "Exception: " + e.getMessage();
            }

            long latency = System.currentTimeMillis() - start;
            totalLatencyMs += latency;

            Map<String, Object> runDetail = new HashMap<>();
            runDetail.put("query", tc.query);
            runDetail.put("passed", success);
            runDetail.put("latencyMs", latency);
            runDetail.put("errorDetail", errorMsg);
            details.add(runDetail);
        }

        double accuracy = runs > 0 ? ((double) passed / runs) : 0.0;

        Map<String, Object> results = new HashMap<>();
        results.put("accuracy", accuracy);
        results.put("totalRuns", runs);
        results.put("passedRuns", passed);
        results.put("averageLatencyMs", runs > 0 ? (totalLatencyMs / runs) : 0);
        results.put("details", details);
        results.put("benchmarkTimestamp", Instant.now().toString());

        log.info("AI Regressions Benchmark completed. Accuracy: {}% (Passed: {}/{})", accuracy * 100.0, passed, runs);
        return results;
    }
}
