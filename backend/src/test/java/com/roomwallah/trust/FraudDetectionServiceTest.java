package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.roomwallah.trust.application.service.FraudDetectionService;
import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.domain.entity.FraudSignal;
import com.roomwallah.trust.domain.port.FraudDetectionPort;
import com.roomwallah.trust.domain.port.FraudSignalRepository;
import com.roomwallah.trust.domain.port.NetworkRiskPort;
import com.roomwallah.trust.domain.valueobject.FraudEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FraudDetectionServiceTest {

    @Mock
    private FraudDetectionPort fraudDetectionPort;

    @Mock
    private FraudSignalRepository fraudSignalRepository;

    @Mock
    private NetworkRiskPort networkRiskPort;

    @Mock
    private TrustScoreService trustScoreService;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        fraudDetectionService = new FraudDetectionService(
                fraudDetectionPort,
                fraudSignalRepository,
                networkRiskPort,
                trustScoreService
        );
    }

    @Test
    public void testRunFraudChecks_SignalsFound_SavesAndRecalculates() {
        UUID userId = UUID.randomUUID();
        
        List<FraudSignal> mockSignals = new ArrayList<>();
        FraudSignal signal = FraudSignal.builder()
                .userId(userId)
                .fraudType("DUPLICATE_ACCOUNT")
                .severity("HIGH")
                .metadataJson("{\"linkedUserId\":\"111\"}")
                .detectedAt(Instant.now())
                .build();
        mockSignals.add(signal);

        when(fraudDetectionPort.detectFraud(userId)).thenReturn(mockSignals);

        // Execute
        List<FraudSignal> result = fraudDetectionService.runFraudChecks(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DUPLICATE_ACCOUNT", result.get(0).getFraudType());

        // Verify DB writes
        verify(fraudSignalRepository).saveAll(mockSignals);

        // Verify recalculate trigger
        verify(trustScoreService).recalculateTrustScore(userId, "FRAUD_DETECTION");
    }

    @Test
    public void testAnalyzeNetworkRisk_SuspiciousIP_SavesSignalAndRecalculates() {
        UUID userId = UUID.randomUUID();
        String suspiciousIp = "198.51.100.42";
        String userAgent = "Mozilla/5.0";

        FraudEvidence mockEvidence = new FraudEvidence("fingerprint_hash", "VPN_DETECTED");
        when(networkRiskPort.analyzeNetworkRisk(userId, suspiciousIp, userAgent)).thenReturn(mockEvidence);

        // Execute
        fraudDetectionService.analyzeNetworkRisk(userId, suspiciousIp, userAgent);

        // Assert/Verify
        ArgumentCaptor<FraudSignal> signalCaptor = ArgumentCaptor.forClass(FraudSignal.class);
        verify(fraudSignalRepository).save(signalCaptor.capture());
        
        FraudSignal savedSignal = signalCaptor.getValue();
        assertEquals(userId, savedSignal.getUserId());
        assertEquals("SUSPICIOUS_IP_RANGE", savedSignal.getFraudType());
        assertEquals("MEDIUM", savedSignal.getSeverity());
        assertTrue(savedSignal.getMetadataJson().contains("fingerprint_hash"));

        // Verify recalculation triggered
        verify(trustScoreService).recalculateTrustScore(userId, "NETWORK_RISK_DETECTED");
    }
}
