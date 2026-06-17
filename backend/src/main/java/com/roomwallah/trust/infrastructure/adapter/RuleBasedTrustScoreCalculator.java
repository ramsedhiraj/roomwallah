package com.roomwallah.trust.infrastructure.adapter;

import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RuleBasedTrustScoreCalculator implements TrustScoreCalculatorPort {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final BrokerDetectionSignalRepository brokerDetectionSignalRepository;

    public RuleBasedTrustScoreCalculator(
            OwnerVerificationRepository ownerVerificationRepository,
            FraudSignalRepository fraudSignalRepository,
            BrokerDetectionSignalRepository brokerDetectionSignalRepository) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.fraudSignalRepository = fraudSignalRepository;
        this.brokerDetectionSignalRepository = brokerDetectionSignalRepository;
    }

    @Override
    public TrustScore calculate(UUID userId) {
        int score = 75; // Default base score for new/unverified users

        // 1. Verification Level bonus
        Optional<OwnerVerification> verificationOpt = ownerVerificationRepository.findByUserId(userId);
        if (verificationOpt.isPresent() && verificationOpt.get().getVerificationStatus() == VerificationStatus.APPROVED) {
            OwnerVerification ov = verificationOpt.get();
            if (ov.getVerificationLevel() == VerificationLevel.VIP) {
                score += 25;
            } else if (ov.getVerificationLevel() == VerificationLevel.STANDARD) {
                score += 20;
            } else {
                score += 15;
            }
        }

        // 2. Fraud Signal penalties
        List<FraudSignal> fraudSignals = fraudSignalRepository.findByUserId(userId);
        for (FraudSignal signal : fraudSignals) {
            String severity = signal.getSeverity().toUpperCase();
            if ("HIGH".equals(severity)) {
                score -= 30;
            } else if ("MEDIUM".equals(severity)) {
                score -= 15;
            } else {
                score -= 5;
            }
        }

        // 3. Broker detection penalties
        List<BrokerDetectionSignal> brokerSignals = brokerDetectionSignalRepository.findByUserId(userId);
        for (BrokerDetectionSignal signal : brokerSignals) {
            BigDecimal weight = signal.getSignalWeight();
            if (weight != null) {
                int penalty = weight.multiply(new BigDecimal("20")).intValue();
                score -= Math.min(penalty, 25);
            }
        }

        // Ensure bounds
        if (score > 100) score = 100;
        if (score < 0) score = 0;

        String explanationJson = String.format(
            "{\"baseScore\":75,\"hasApprovedVerification\":%b,\"fraudSignalsCount\":%d,\"brokerSignalsCount\":%d}",
            verificationOpt.isPresent() && verificationOpt.get().getVerificationStatus() == VerificationStatus.APPROVED,
            fraudSignals.size(),
            brokerSignals.size()
        );

        return TrustScore.builder()
                .userId(userId)
                .currentScore(score)
                .scoreVersion(1)
                .ruleVersion("v2.0")
                .algorithmVersion("rule-based-v2")
                .explanationJson(explanationJson)
                // Backward compatibility fields
                .overallScore(score)
                .identityScore(verificationOpt.isPresent() && verificationOpt.get().getVerificationStatus() == VerificationStatus.APPROVED ? 90 : 50)
                .propertyScore(80)
                .reviewScore(85)
                .activityScore(90)
                .fraudPenalty(fraudSignals.size() * 10)
                .calculatedAt(Instant.now())
                .build();
    }
}
