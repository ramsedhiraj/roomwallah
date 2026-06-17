package com.roomwallah.trust.application.service;

import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.domain.entity.TrustScoreHistory;
import com.roomwallah.trust.domain.event.TrustScoreChangedEvent;
import com.roomwallah.trust.domain.port.TrustScoreCalculatorPort;
import com.roomwallah.trust.domain.port.TrustScoreHistoryRepository;
import com.roomwallah.trust.domain.port.TrustScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TrustScoreService {

    private final TrustScoreRepository trustScoreRepository;
    private final TrustScoreHistoryRepository trustScoreHistoryRepository;
    private final TrustScoreCalculatorPort trustScoreCalculator;
    private final OutboxEventPublisher outboxEventPublisher;

    public TrustScoreService(
            TrustScoreRepository trustScoreRepository,
            TrustScoreHistoryRepository trustScoreHistoryRepository,
            TrustScoreCalculatorPort trustScoreCalculator,
            OutboxEventPublisher outboxEventPublisher) {
        this.trustScoreRepository = trustScoreRepository;
        this.trustScoreHistoryRepository = trustScoreHistoryRepository;
        this.trustScoreCalculator = trustScoreCalculator;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public TrustScore recalculateTrustScore(UUID userId, String triggerEvent) {
        log.info("Recalculating trust score for user: {}, trigger: {}", userId, triggerEvent);

        TrustScore newScoreData = trustScoreCalculator.calculate(userId);

        Optional<TrustScore> existingOpt = trustScoreRepository.findByUserId(userId);
        int oldScoreValue = existingOpt.map(TrustScore::getCurrentScore).orElse(75);

        TrustScore trustScoreToSave;
        if (existingOpt.isPresent()) {
            trustScoreToSave = existingOpt.get();
            trustScoreToSave.setCurrentScore(newScoreData.getCurrentScore());
            trustScoreToSave.setScoreVersion(newScoreData.getScoreVersion());
            trustScoreToSave.setRuleVersion(newScoreData.getRuleVersion());
            trustScoreToSave.setAlgorithmVersion(newScoreData.getAlgorithmVersion());
            trustScoreToSave.setExplanationJson(newScoreData.getExplanationJson());
            // Old fields
            trustScoreToSave.setOverallScore(newScoreData.getOverallScore());
            trustScoreToSave.setIdentityScore(newScoreData.getIdentityScore());
            trustScoreToSave.setPropertyScore(newScoreData.getPropertyScore());
            trustScoreToSave.setReviewScore(newScoreData.getReviewScore());
            trustScoreToSave.setActivityScore(newScoreData.getActivityScore());
            trustScoreToSave.setFraudPenalty(newScoreData.getFraudPenalty());
            trustScoreToSave.setCalculatedAt(Instant.now());
        } else {
            trustScoreToSave = newScoreData;
        }

        trustScoreToSave = trustScoreRepository.save(trustScoreToSave);

        // Record history
        TrustScoreHistory history = TrustScoreHistory.builder()
                .userId(userId)
                .previousScore(oldScoreValue)
                .newScore(newScoreData.getCurrentScore())
                .reason("Recalculation triggered by: " + triggerEvent)
                .triggeredByEvent(triggerEvent)
                .calculatedAt(Instant.now())
                .build();
        trustScoreHistoryRepository.save(history);

        // Publish Outbox event if score changed
        if (oldScoreValue != newScoreData.getCurrentScore()) {
            outboxEventPublisher.persistEvent("TrustScore", trustScoreToSave.getId().toString(),
                    new TrustScoreChangedEvent(userId, oldScoreValue, newScoreData.getCurrentScore(), Instant.now()));
            log.info("Trust score changed from {} to {} for user: {}", oldScoreValue, newScoreData.getCurrentScore(), userId);
        }

        return trustScoreToSave;
    }

    public Optional<TrustScore> getTrustScore(UUID userId) {
        return trustScoreRepository.findByUserId(userId);
    }
}
