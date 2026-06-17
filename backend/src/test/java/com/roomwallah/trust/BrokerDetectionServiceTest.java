package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.roomwallah.trust.application.service.BrokerDetectionService;
import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.domain.entity.BrokerDetectionSignal;
import com.roomwallah.trust.domain.port.BrokerDetectionPort;
import com.roomwallah.trust.domain.port.BrokerDetectionSignalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BrokerDetectionServiceTest {

    @Mock
    private BrokerDetectionPort brokerDetectionPort;

    @Mock
    private BrokerDetectionSignalRepository brokerDetectionSignalRepository;

    @Mock
    private TrustScoreService trustScoreService;

    private BrokerDetectionService brokerDetectionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        brokerDetectionService = new BrokerDetectionService(
                brokerDetectionPort,
                brokerDetectionSignalRepository,
                trustScoreService
        );
    }

    @Test
    public void testRunDetection_SignalsFound_SavesAndRecalculates() {
        UUID userId = UUID.randomUUID();
        
        List<BrokerDetectionSignal> mockSignals = new ArrayList<>();
        BrokerDetectionSignal signal = BrokerDetectionSignal.builder()
                .userId(userId)
                .signalType("EXCESSIVE_LISTINGS")
                .signalWeight(new BigDecimal("3.5000"))
                .metadataJson("{\"listingsCount\": 12}")
                .detectedAt(Instant.now())
                .build();
        mockSignals.add(signal);

        when(brokerDetectionPort.detect(userId)).thenReturn(mockSignals);

        // Execute
        List<BrokerDetectionSignal> result = brokerDetectionService.runDetection(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EXCESSIVE_LISTINGS", result.get(0).getSignalType());

        // Verify saveAll call
        verify(brokerDetectionSignalRepository).saveAll(mockSignals);

        // Verify trust score recalculation is triggered
        verify(trustScoreService).recalculateTrustScore(userId, "BROKER_DETECTION");
    }

    @Test
    public void testRunDetection_NoSignals_DoesNotSaveOrRecalculate() {
        UUID userId = UUID.randomUUID();
        when(brokerDetectionPort.detect(userId)).thenReturn(new ArrayList<>());

        // Execute
        List<BrokerDetectionSignal> result = brokerDetectionService.runDetection(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify no DB or recalculate call
        verify(brokerDetectionSignalRepository, never()).saveAll(any());
        verify(trustScoreService, never()).recalculateTrustScore(any(), anyString());
    }
}
