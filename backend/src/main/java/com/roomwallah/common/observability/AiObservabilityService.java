package com.roomwallah.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiObservabilityService {

    private final MeterRegistry meterRegistry;
    private final TenantAiQuotaRepository quotaRepository;

    // In-memory aggregates for the monitoring dashboard
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tokenCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalLatency = new ConcurrentHashMap<>();
    
    // Cost calculation rules per million tokens
    private static final double EMBEDDING_COST_PER_MILLION = 0.02;
    private static final double MINI_INPUT_COST_PER_MILLION = 0.15;
    private static final double MINI_OUTPUT_COST_PER_MILLION = 0.60;
    private static final double PRO_INPUT_COST_PER_MILLION = 5.00;
    private static final double PRO_OUTPUT_COST_PER_MILLION = 15.00;

    private final Map<String, DoubleAccumulator> costTracker = new ConcurrentHashMap<>();

    // Additional observability metrics:
    private final DoubleAccumulator semanticSearchConfidenceSum = new DoubleAccumulator(Double::sum, 0.0);
    private final AtomicInteger semanticSearchCount = new AtomicInteger(0);
    
    private final AtomicInteger recommendationClicks = new AtomicInteger(0);
    private final AtomicInteger recommendationImpressions = new AtomicInteger(0);
    
    private final AtomicInteger duplicateDetectionRuns = new AtomicInteger(0);
    private final AtomicInteger duplicateDetectionsConfirmed = new AtomicInteger(0);
    
    private final AtomicInteger assistantCalls = new AtomicInteger(0);
    private final AtomicInteger assistantFallbacks = new AtomicInteger(0);
    private final AtomicInteger tenantIsolationFailures = new AtomicInteger(0);
    
    private final AtomicInteger safetyViolations = new AtomicInteger(0);
    private final AtomicInteger humanOverrides = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final DoubleAccumulator hallucinationRiskSum = new DoubleAccumulator(Double::sum, 0.0);
    private final AtomicInteger hallucinationRiskCount = new AtomicInteger(0);
    private final DoubleAccumulator vectorStorageCost = new DoubleAccumulator(Double::sum, 0.0);

    @Transactional(readOnly = true)
    public void verifyTenantQuota(String tenantId, double costEstimate) {
        String tId = tenantId != null ? tenantId : "default";
        TenantAiQuota quota = quotaRepository.findById(tId).orElse(null);
        if (quota != null) {
            BigDecimal proposedSpend = quota.getCurrentSpendUsd().add(BigDecimal.valueOf(costEstimate));
            if (proposedSpend.compareTo(quota.getMonthlyLimitUsd()) > 0) {
                log.warn("AI Quota Exceeded for Tenant: {}. Limit: ${}, Proposed Spend: ${}", 
                        tId, quota.getMonthlyLimitUsd(), proposedSpend);
                throw new IllegalStateException("AI Quota Exceeded for tenant: " + tId);
            }
        }
    }

    @Transactional
    public void trackRequest(UUID userId, String tenantId, String model, int inputTokens, int outputTokens, long latencyMs, boolean success, String errorMsg) {
        String tId = tenantId != null ? tenantId : "default";
        String key = tId + ":" + model;
        
        requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        if (!success) {
            failureCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        }
        tokenCounts.computeIfAbsent(key + ":input", k -> new AtomicLong(0)).addAndGet(inputTokens);
        tokenCounts.computeIfAbsent(key + ":output", k -> new AtomicLong(0)).addAndGet(outputTokens);
        totalLatency.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(latencyMs);

        // Compute cost
        double cost = calculateCost(model, inputTokens, outputTokens);
        costTracker.computeIfAbsent(tId, k -> new DoubleAccumulator(Double::sum, 0.0)).accumulate(cost);

        // Update database quota records
        TenantAiQuota quota = quotaRepository.findById(tId).orElse(null);
        if (quota != null) {
            quota.setCurrentSpendUsd(quota.getCurrentSpendUsd().add(BigDecimal.valueOf(cost)));
            quota.setUpdatedAt(Instant.now());
            quotaRepository.save(quota);
            if (quota.getCurrentSpendUsd().compareTo(quota.getMonthlyLimitUsd().multiply(BigDecimal.valueOf(0.9))) > 0) {
                log.warn("ALERT: Tenant {} AI spend of ${} has exceeded 90% of monthly limit ${}!", 
                        tId, quota.getCurrentSpendUsd(), quota.getMonthlyLimitUsd());
            }
        }

        // Micrometer metrics
        if (meterRegistry != null) {
            meterRegistry.counter("ai.requests", "tenant", tId, "model", model, "success", String.valueOf(success)).increment();
            meterRegistry.timer("ai.latency", "tenant", tId, "model", model).record(java.time.Duration.ofMillis(latencyMs));
            meterRegistry.counter("ai.tokens", "tenant", tId, "type", "input").increment(inputTokens);
            meterRegistry.counter("ai.tokens", "tenant", tId, "type", "output").increment(outputTokens);
        }

        log.info("AI Usage Tracked: Tenant={}, Model={}, Success={}, InputTokens={}, OutputTokens={}, Cost=${}, Latency={}ms", 
                tId, model, success, inputTokens, outputTokens, cost, latencyMs);
    }

    private double calculateCost(String model, int inputTokens, int outputTokens) {
        if (model == null) return 0.0;
        
        double cost = 0.0;
        if (model.contains("embedding")) {
            cost += ((double) inputTokens / 1_000_000.0) * EMBEDDING_COST_PER_MILLION;
        } else if (model.contains("mini")) {
            cost += ((double) inputTokens / 1_000_000.0) * MINI_INPUT_COST_PER_MILLION;
            cost += ((double) outputTokens / 1_000_000.0) * MINI_OUTPUT_COST_PER_MILLION;
        } else {
            // Default to GPT-4o pro pricing
            cost += ((double) inputTokens / 1_000_000.0) * PRO_INPUT_COST_PER_MILLION;
            cost += ((double) outputTokens / 1_000_000.0) * PRO_OUTPUT_COST_PER_MILLION;
        }
        return BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    public void trackSemanticSearchConfidence(double confidence) {
        semanticSearchConfidenceSum.accumulate(confidence);
        semanticSearchCount.incrementAndGet();
    }

    public void trackRecommendationInteraction(boolean click) {
        recommendationImpressions.incrementAndGet();
        if (click) {
            recommendationClicks.incrementAndGet();
        }
    }

    public void trackDuplicateDetection(boolean duplicateConfirmed) {
        duplicateDetectionRuns.incrementAndGet();
        if (duplicateConfirmed) {
            duplicateDetectionsConfirmed.incrementAndGet();
        }
    }

    public void trackAssistantCall(boolean fallback) {
        assistantCalls.incrementAndGet();
        if (fallback) {
            assistantFallbacks.incrementAndGet();
        }
    }

    public void trackSafetyViolation() {
        safetyViolations.incrementAndGet();
    }

    public void trackHumanOverride() {
        humanOverrides.incrementAndGet();
    }

    public void trackCacheLookup(boolean isHit) {
        if (isHit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
    }

    public void trackHallucination(double riskScore) {
        hallucinationRiskSum.accumulate(riskScore);
        hallucinationRiskCount.incrementAndGet();
    }

    public void trackVectorStorageCost(double cost) {
        vectorStorageCost.accumulate(cost);
    }

    public void trackTenantIsolationFailure() {
        tenantIsolationFailures.incrementAndGet();
    }

    public Map<String, Object> getDashboardStats(String tenantId) {
        String tId = tenantId != null ? tenantId : "default";
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        double currentSpend = 0.0;
        double budgetLimit = 500.0;
        
        TenantAiQuota quota = quotaRepository.findById(tId).orElse(null);
        if (quota != null) {
            currentSpend = quota.getCurrentSpendUsd().doubleValue();
            budgetLimit = quota.getMonthlyLimitUsd().doubleValue();
        }

        stats.put("totalCostUsd", BigDecimal.valueOf(currentSpend).setScale(4, RoundingMode.HALF_UP));
        stats.put("monthlyLimitUsd", BigDecimal.valueOf(budgetLimit).setScale(4, RoundingMode.HALF_UP));
        
        int requests = 0;
        int failures = 0;
        long tokens = 0;
        long latencySum = 0;
        
        for (String k : requestCounts.keySet()) {
            if (k.startsWith(tId + ":")) {
                requests += requestCounts.get(k).get();
                failures += failureCounts.getOrDefault(k, new AtomicInteger(0)).get();
                latencySum += totalLatency.getOrDefault(k, new AtomicLong(0)).get();
            }
        }
        
        for (String k : tokenCounts.keySet()) {
            if (k.startsWith(tId + ":")) {
                tokens += tokenCounts.get(k).get();
            }
        }
        
        stats.put("requestCount", requests);
        stats.put("failureCount", failures);
        stats.put("totalTokens", tokens);
        stats.put("averageLatencyMs", requests > 0 ? (latencySum / requests) : 0);
        stats.put("failureRate", requests > 0 ? ((double) failures / requests) : 0.0);

        // AI Observability Metrics
        stats.put("avgSemanticSearchConfidence", semanticSearchCount.get() > 0 ? 
                (semanticSearchConfidenceSum.get() / semanticSearchCount.get()) : 1.0);
        stats.put("recommendationCtr", recommendationImpressions.get() > 0 ? 
                ((double) recommendationClicks.get() / recommendationImpressions.get()) : 0.0);
        stats.put("duplicateDetectionPrecision", duplicateDetectionRuns.get() > 0 ? 
                ((double) duplicateDetectionsConfirmed.get() / duplicateDetectionRuns.get()) : 1.0);
        stats.put("assistantFallbackRate", assistantCalls.get() > 0 ? 
                ((double) assistantFallbacks.get() / assistantCalls.get()) : 0.0);
        stats.put("tenantIsolationFailures", tenantIsolationFailures.get());
        
        stats.put("safetyViolations", safetyViolations.get());
        stats.put("humanOverrides", humanOverrides.get());
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        stats.put("vectorStorageCost", vectorStorageCost.get());
        stats.put("avgHallucinationRisk", hallucinationRiskCount.get() > 0 ? 
                (hallucinationRiskSum.get() / hallucinationRiskCount.get()) : 0.0);

        return stats;
    }
}
