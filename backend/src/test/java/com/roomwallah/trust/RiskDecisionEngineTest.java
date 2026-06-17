package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.*;
import com.roomwallah.trust.domain.valueobject.RiskAssessment;
import com.roomwallah.trust.infrastructure.adapter.RuleBasedRiskDecisionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RiskDecisionEngineTest {

    @Mock
    private TrustScoreRepository trustScoreRepository;

    @Mock
    private FraudSignalRepository fraudSignalRepository;

    @Mock
    private BrokerDetectionSignalRepository brokerDetectionSignalRepository;

    @Mock
    private OwnerVerificationRepository ownerVerificationRepository;

    private RuleBasedRiskDecisionEngine riskDecisionEngine;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        riskDecisionEngine = new RuleBasedRiskDecisionEngine(
                trustScoreRepository,
                fraudSignalRepository,
                brokerDetectionSignalRepository,
                ownerVerificationRepository
        );
    }

    @Test
    public void testAssess_ReturnsAllow_WhenProfileWithinBounds() {
        UUID userId = UUID.randomUUID();

        // 85 trust score
        TrustScore trustScore = new TrustScore();
        trustScore.setCurrentScore(85);
        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(trustScore));

        // No fraud / broker signals
        when(fraudSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(brokerDetectionSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(ownerVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Execute
        RiskAssessment assessment = riskDecisionEngine.assess(userId);

        // Assert
        assertNotNull(assessment);
        assertEquals(RiskDecision.ALLOW, assessment.getDecision());
        assertEquals(85, assessment.getTrustScore());
    }

    @Test
    public void testAssess_ReturnsBlock_WhenHasCriticalFraud() {
        UUID userId = UUID.randomUUID();

        TrustScore trustScore = new TrustScore();
        trustScore.setCurrentScore(85);
        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(trustScore));

        // Critical fraud signal
        List<FraudSignal> fraudSignals = new ArrayList<>();
        FraudSignal criticalSig = FraudSignal.builder()
                .userId(userId)
                .severity("CRITICAL")
                .fraudType("FAKE_IDENTITY")
                .build();
        fraudSignals.add(criticalSig);

        when(fraudSignalRepository.findByUserId(userId)).thenReturn(fraudSignals);
        when(brokerDetectionSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(ownerVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Execute
        RiskAssessment assessment = riskDecisionEngine.assess(userId);

        // Assert
        assertNotNull(assessment);
        assertEquals(RiskDecision.BLOCK, assessment.getDecision());
    }

    @Test
    public void testAssess_ReturnsRestrict_WhenBrokerScoreIsHigh() {
        UUID userId = UUID.randomUUID();

        TrustScore trustScore = new TrustScore();
        trustScore.setCurrentScore(85);
        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(trustScore));
        when(fraudSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());

        // High broker signal weights (total weight 9.0 -> 90 score)
        List<BrokerDetectionSignal> brokerSignals = new ArrayList<>();
        brokerSignals.add(BrokerDetectionSignal.builder()
                .userId(userId)
                .signalType("EXCESSIVE_LISTINGS")
                .signalWeight(new BigDecimal("9.0000"))
                .build());

        when(brokerDetectionSignalRepository.findByUserId(userId)).thenReturn(brokerSignals);
        when(ownerVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Execute
        RiskAssessment assessment = riskDecisionEngine.assess(userId);

        // Assert
        assertNotNull(assessment);
        assertEquals(RiskDecision.RESTRICT, assessment.getDecision());
        assertTrue(assessment.getBrokerScore() >= 80);
    }

    @Test
    public void testAssess_ReturnsRequireReview_WhenTrustScoreIsLow() {
        UUID userId = UUID.randomUUID();

        // Low trust score (15)
        TrustScore trustScore = new TrustScore();
        trustScore.setCurrentScore(15);
        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(trustScore));

        when(fraudSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(brokerDetectionSignalRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(ownerVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Execute
        RiskAssessment assessment = riskDecisionEngine.assess(userId);

        // Assert
        assertNotNull(assessment);
        assertEquals(RiskDecision.REQUIRE_REVIEW, assessment.getDecision());
    }
}
