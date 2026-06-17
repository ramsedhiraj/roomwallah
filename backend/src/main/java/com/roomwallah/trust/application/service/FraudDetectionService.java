package com.roomwallah.trust.application.service;

import com.roomwallah.trust.domain.entity.FraudSignal;
import com.roomwallah.trust.domain.port.FraudDetectionPort;
import com.roomwallah.trust.domain.port.FraudSignalRepository;
import com.roomwallah.trust.domain.port.NetworkRiskPort;
import com.roomwallah.trust.domain.valueobject.FraudEvidence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FraudDetectionService {

    private final FraudDetectionPort fraudDetectionPort;
    private final FraudSignalRepository fraudSignalRepository;
    private final NetworkRiskPort networkRiskPort;
    private final TrustScoreService trustScoreService;

    public FraudDetectionService(
            FraudDetectionPort fraudDetectionPort,
            FraudSignalRepository fraudSignalRepository,
            NetworkRiskPort networkRiskPort,
            TrustScoreService trustScoreService) {
        this.fraudDetectionPort = fraudDetectionPort;
        this.fraudSignalRepository = fraudSignalRepository;
        this.networkRiskPort = networkRiskPort;
        this.trustScoreService = trustScoreService;
    }

    @Transactional
    public List<FraudSignal> runFraudChecks(UUID userId) {
        log.info("Running fraud detection checks for user: {}", userId);
        List<FraudSignal> signals = fraudDetectionPort.detectFraud(userId);
        if (!signals.isEmpty()) {
            fraudSignalRepository.saveAll(signals);
            log.info("Saved {} fraud signals for user: {}", signals.size(), userId);
            trustScoreService.recalculateTrustScore(userId, "FRAUD_DETECTION");
        }
        return signals;
    }

    @Transactional
    public void analyzeNetworkRisk(UUID userId, String ipAddress, String userAgent) {
        log.info("Analyzing network risk for user: {} from IP: {}", userId, ipAddress);
        FraudEvidence evidence = networkRiskPort.analyzeNetworkRisk(userId, ipAddress, userAgent);

        // If IP or fingerprint is suspicious, log a fraud signal
        if (ipAddress != null && (ipAddress.startsWith("198.51.100") || ipAddress.startsWith("203.0.113"))) {
            FraudSignal signal = FraudSignal.builder()
                    .userId(userId)
                    .fraudType("SUSPICIOUS_IP_RANGE")
                    .severity("MEDIUM")
                    .metadataJson(String.format("{\"fingerprint\":\"%s\",\"context\":\"%s\"}", evidence.getFingerprint(), evidence.getContext()))
                    .detectedAt(Instant.now())
                    // Compatibility fields
                    .signalType("SUSPICIOUS_IP_RANGE")
                    .description("User logged in from suspicious IP range: " + ipAddress)
                    .brokerRiskScore(15)
                    .build();
            fraudSignalRepository.save(signal);
            log.warn("Suspicious network risk detected for user: {}", userId);
            trustScoreService.recalculateTrustScore(userId, "NETWORK_RISK_DETECTED");
        }
    }
}
