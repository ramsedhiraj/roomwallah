package com.roomwallah.verification.application.service;

import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.verification.domain.entity.FraudSignal;
import com.roomwallah.verification.domain.entity.SeverityLevel;
import com.roomwallah.verification.domain.event.FraudSignalDetectedEvent;
import com.roomwallah.verification.domain.port.EventPublisherPort;
import com.roomwallah.verification.domain.repository.FraudSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerDetectionServiceImpl implements BrokerDetectionService {

    private final BrokerRiskCalculationService riskCalculationService;
    private final FraudSignalRepository fraudSignalRepository;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void detectBrokerPatterns(UUID userId) {
        log.info("Running broker detection patterns for user: {}", userId);
        int riskScore = riskCalculationService.calculateRiskScore(userId);

        if (riskScore > 0) {
            SeverityLevel severity;
            if (riskScore >= 80) {
                severity = SeverityLevel.CRITICAL;
            } else if (riskScore >= 50) {
                severity = SeverityLevel.HIGH;
            } else if (riskScore >= 30) {
                severity = SeverityLevel.MEDIUM;
            } else {
                severity = SeverityLevel.LOW;
            }

            FraudSignal signal = new FraudSignal();
            signal.setUserId(userId);
            signal.setSignalType("BROKER_RISK_INDEX");
            signal.setSeverity(severity);
            signal.setBrokerRiskScore(riskScore);
            signal.setDescription("System flagged broker risk indices: score " + riskScore + "/100");
            signal.setCreatedAt(Instant.now(clock));

            signal = fraudSignalRepository.save(signal);
            log.warn("Persisted fraud signal ID: {} with severity: {} for user: {}", signal.getId(), severity, userId);

            // Publish event to Outbox
            eventPublisher.publish(new FraudSignalDetectedEvent(
                userId,
                signal.getSignalType(),
                severity,
                riskScore,
                CorrelationContext.get(),
                Instant.now(clock)
            ));
        } else {
            // No risk detected, clear previous signals if any (or keep them for history)
            // The prompt says "Generate risk scores and flags without automatic banning", keeping signals is standard for compliance.
            log.info("No broker risk patterns flagged for user: {}", userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudSignal> getFraudSignals(UUID userId) {
        return fraudSignalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudSignal> getAllFraudSignals() {
        return fraudSignalRepository.findAllByOrderByCreatedAtDesc();
    }
}
