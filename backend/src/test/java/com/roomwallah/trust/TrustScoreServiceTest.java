package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.roomwallah.trust.application.service.OutboxEventPublisher;
import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.domain.entity.TrustScoreHistory;
import com.roomwallah.trust.domain.port.TrustScoreCalculatorPort;
import com.roomwallah.trust.domain.port.TrustScoreHistoryRepository;
import com.roomwallah.trust.domain.port.TrustScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TrustScoreServiceTest {

    @Mock
    private TrustScoreRepository trustScoreRepository;

    @Mock
    private TrustScoreHistoryRepository trustScoreHistoryRepository;

    @Mock
    private TrustScoreCalculatorPort trustScoreCalculator;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private TrustScoreService trustScoreService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        trustScoreService = new TrustScoreService(
                trustScoreRepository,
                trustScoreHistoryRepository,
                trustScoreCalculator,
                outboxEventPublisher
        );
    }

    @Test
    public void testRecalculateScore_ScoreChanged_PublishesEventAndLogsHistory() {
        UUID userId = UUID.randomUUID();
        
        // Setup existing score
        TrustScore existingScore = new TrustScore();
        existingScore.setId(UUID.randomUUID());
        existingScore.setUserId(userId);
        existingScore.setCurrentScore(75);
        existingScore.setOverallScore(75);
        
        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(existingScore));

        // Setup new calculated score
        TrustScore newCalculatedScore = new TrustScore();
        newCalculatedScore.setId(existingScore.getId());
        newCalculatedScore.setUserId(userId);
        newCalculatedScore.setCurrentScore(85);
        newCalculatedScore.setRuleVersion("v1.0.0");
        newCalculatedScore.setAlgorithmVersion("v1.0.0");
        newCalculatedScore.setExplanationJson("{\"verified\":true}");

        when(trustScoreCalculator.calculate(userId)).thenReturn(newCalculatedScore);
        when(trustScoreRepository.save(any(TrustScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        TrustScore result = trustScoreService.recalculateTrustScore(userId, "TEST_EVENT");

        // Asserts
        assertNotNull(result);
        assertEquals(85, result.getCurrentScore());
        assertEquals("v1.0.0", result.getRuleVersion());

        // Verify save calls
        verify(trustScoreRepository).save(existingScore);

        // Verify history logs
        ArgumentCaptor<TrustScoreHistory> historyCaptor = ArgumentCaptor.forClass(TrustScoreHistory.class);
        verify(trustScoreHistoryRepository).save(historyCaptor.capture());
        TrustScoreHistory savedHistory = historyCaptor.getValue();
        assertEquals(userId, savedHistory.getUserId());
        assertEquals(75, savedHistory.getPreviousScore());
        assertEquals(85, savedHistory.getNewScore());
        assertEquals("TEST_EVENT", savedHistory.getTriggeredByEvent());

        // Verify outbox publishes
        verify(outboxEventPublisher).persistEvent(eq("TrustScore"), eq(existingScore.getId().toString()), any());
    }

    @Test
    public void testRecalculateScore_ScoreUnchanged_NoEventPublished() {
        UUID userId = UUID.randomUUID();

        // Setup existing score
        TrustScore existingScore = new TrustScore();
        existingScore.setId(UUID.randomUUID());
        existingScore.setUserId(userId);
        existingScore.setCurrentScore(80);
        existingScore.setOverallScore(80);

        when(trustScoreRepository.findByUserId(userId)).thenReturn(Optional.of(existingScore));

        // Setup new calculated score (same value)
        TrustScore newCalculatedScore = new TrustScore();
        newCalculatedScore.setId(existingScore.getId());
        newCalculatedScore.setUserId(userId);
        newCalculatedScore.setCurrentScore(80);

        when(trustScoreCalculator.calculate(userId)).thenReturn(newCalculatedScore);
        when(trustScoreRepository.save(any(TrustScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        TrustScore result = trustScoreService.recalculateTrustScore(userId, "TEST_EVENT");

        // Asserts
        assertNotNull(result);
        assertEquals(80, result.getCurrentScore());

        // Verify history still logged
        verify(trustScoreHistoryRepository).save(any(TrustScoreHistory.class));

        // Verify NO outbox publishes since score did not change
        verify(outboxEventPublisher, never()).persistEvent(anyString(), anyString(), any());
    }
}
