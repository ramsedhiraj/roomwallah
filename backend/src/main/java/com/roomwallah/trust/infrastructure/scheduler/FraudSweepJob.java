package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.application.service.FraudDetectionService;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.port.OwnerVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class FraudSweepJob {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final FraudDetectionService fraudDetectionService;

    public FraudSweepJob(OwnerVerificationRepository ownerVerificationRepository, FraudDetectionService fraudDetectionService) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Scheduled(cron = "${roomwallah.trust.fraud-sweep.cron:0 0/30 * * * ?}")
    public void run() {
        log.info("Starting FraudSweepJob...");
        List<OwnerVerification> verifications = ownerVerificationRepository.findAll();
        for (OwnerVerification ov : verifications) {
            try {
                fraudDetectionService.runFraudChecks(ov.getUserId());
            } catch (Exception e) {
                log.error("Failed to run fraud sweep for user: {}", ov.getUserId(), e);
            }
        }
        log.info("Finished FraudSweepJob.");
    }
}
