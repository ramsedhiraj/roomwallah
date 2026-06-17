package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.application.service.BrokerDetectionService;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.port.OwnerVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class BrokerDetectionJob {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final BrokerDetectionService brokerDetectionService;

    public BrokerDetectionJob(OwnerVerificationRepository ownerVerificationRepository, BrokerDetectionService brokerDetectionService) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.brokerDetectionService = brokerDetectionService;
    }

    @Scheduled(cron = "${roomwallah.trust.broker-detect.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting BrokerDetectionJob...");
        List<OwnerVerification> verifications = ownerVerificationRepository.findAll();
        for (OwnerVerification ov : verifications) {
            try {
                brokerDetectionService.runDetection(ov.getUserId());
            } catch (Exception e) {
                log.error("Failed to run broker detection for user: {}", ov.getUserId(), e);
            }
        }
        log.info("Finished BrokerDetectionJob.");
    }
}
