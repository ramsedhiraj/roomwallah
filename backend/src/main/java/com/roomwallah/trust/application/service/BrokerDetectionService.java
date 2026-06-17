package com.roomwallah.trust.application.service;

import com.roomwallah.trust.domain.entity.BrokerDetectionSignal;
import com.roomwallah.trust.domain.port.BrokerDetectionPort;
import com.roomwallah.trust.domain.port.BrokerDetectionSignalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BrokerDetectionService {

    private final BrokerDetectionPort brokerDetectionPort;
    private final BrokerDetectionSignalRepository brokerDetectionSignalRepository;
    private final TrustScoreService trustScoreService;

    public BrokerDetectionService(
            BrokerDetectionPort brokerDetectionPort,
            BrokerDetectionSignalRepository brokerDetectionSignalRepository,
            TrustScoreService trustScoreService) {
        this.brokerDetectionPort = brokerDetectionPort;
        this.brokerDetectionSignalRepository = brokerDetectionSignalRepository;
        this.trustScoreService = trustScoreService;
    }

    @Transactional
    public List<BrokerDetectionSignal> runDetection(UUID userId) {
        log.info("Running broker detection for user: {}", userId);
        List<BrokerDetectionSignal> signals = brokerDetectionPort.detect(userId);
        if (!signals.isEmpty()) {
            brokerDetectionSignalRepository.saveAll(signals);
            log.info("Saved {} broker detection signals for user: {}", signals.size(), userId);
            trustScoreService.recalculateTrustScore(userId, "BROKER_DETECTION");
        }
        return signals;
    }
}
