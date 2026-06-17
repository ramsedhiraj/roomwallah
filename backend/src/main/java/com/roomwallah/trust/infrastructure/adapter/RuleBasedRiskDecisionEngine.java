package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.*;
import com.roomwallah.trust.domain.valueobject.RiskAssessment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RuleBasedRiskDecisionEngine implements RiskDecisionEnginePort {

    private final TrustScoreRepository trustScoreRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final BrokerDetectionSignalRepository brokerDetectionSignalRepository;
    private final OwnerVerificationRepository ownerVerificationRepository;

    public RuleBasedRiskDecisionEngine(
            TrustScoreRepository trustScoreRepository,
            FraudSignalRepository fraudSignalRepository,
            BrokerDetectionSignalRepository brokerDetectionSignalRepository,
            OwnerVerificationRepository ownerVerificationRepository) {
        this.trustScoreRepository = trustScoreRepository;
        this.fraudSignalRepository = fraudSignalRepository;
        this.brokerDetectionSignalRepository = brokerDetectionSignalRepository;
        this.ownerVerificationRepository = ownerVerificationRepository;
    }

    @Override
    public RiskAssessment assess(UUID userId) {
        log.info("Running unified risk assessment for user: {}", userId);

        // Fetch current trust score (default to 50 if missing)
        int trustScoreVal = trustScoreRepository.findByUserId(userId)
                .map(TrustScore::getCurrentScore)
                .orElse(50);

        // Fetch fraud signals
        List<FraudSignal> fraudSignals = fraudSignalRepository.findByUserId(userId);
        int maxFraudSeverityWeight = 0;
        boolean hasCriticalFraud = false;
        
        for (FraudSignal sig : fraudSignals) {
            String severity = sig.getSeverity() != null ? sig.getSeverity() : "LOW";
            if ("CRITICAL".equalsIgnoreCase(severity)) {
                hasCriticalFraud = true;
            }
            int weight = getFraudWeight(severity);
            if (weight > maxFraudSeverityWeight) {
                maxFraudSeverityWeight = weight;
            }
        }

        // Fetch broker signals
        List<BrokerDetectionSignal> brokerSignals = brokerDetectionSignalRepository.findByUserId(userId);
        double totalBrokerWeight = 0.0;
        for (BrokerDetectionSignal sig : brokerSignals) {
            totalBrokerWeight += sig.getSignalWeight() != null ? sig.getSignalWeight().doubleValue() : 0.0;
        }
        int brokerScoreVal = (int) Math.min(100.0, totalBrokerWeight * 10.0);

        // Fetch active owner verification status
        String verifStatus = ownerVerificationRepository.findByUserId(userId)
                .map(v -> v.getVerificationStatus().name())
                .orElse("NONE");

        // Determine final decision
        RiskDecision decision = RiskDecision.ALLOW;
        String explanation = "Profile parameters fall within standard bounds.";

        if (hasCriticalFraud) {
            decision = RiskDecision.BLOCK;
            explanation = "Critical fraud signal detected on user profile.";
        } else if (maxFraudSeverityWeight >= 70 || brokerScoreVal >= 80) {
            decision = RiskDecision.RESTRICT;
            explanation = "High broker patterns or severe fraud signals detected. Restricting active postings.";
        } else if (trustScoreVal < 30 || "REJECTED".equalsIgnoreCase(verifStatus) || maxFraudSeverityWeight >= 30) {
            decision = RiskDecision.REQUIRE_REVIEW;
            explanation = "Trust score below threshold or verification rejected. Esculating for admin manual check.";
        }

        log.info("Risk assessment finished for user: {}. Decision: {}, Explanation: {}", userId, decision, explanation);

        return new RiskAssessment(
                trustScoreVal,
                maxFraudSeverityWeight,
                brokerScoreVal,
                decision,
                explanation
        );
    }

    private int getFraudWeight(String severity) {
        if (severity == null) return 10;
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 100;
            case "HIGH" -> 80;
            case "MEDIUM" -> 40;
            default -> 10;
        };
    }
}
