package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.application.service.OutboxEventPublisher;
import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.entity.VerificationStatus;
import com.roomwallah.trust.domain.event.VerificationExpiredEvent;
import com.roomwallah.trust.domain.port.OwnerVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class VerificationExpiryJob {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final TrustScoreService trustScoreService;
    private final OutboxEventPublisher outboxEventPublisher;

    public VerificationExpiryJob(
            OwnerVerificationRepository ownerVerificationRepository,
            TrustScoreService trustScoreService,
            OutboxEventPublisher outboxEventPublisher) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.trustScoreService = trustScoreService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Scheduled(cron = "${roomwallah.trust.expiry-sweep.cron:0 0 1 * * ?}")
    @Transactional
    public void run() {
        log.info("Starting VerificationExpiryJob...");
        Instant now = Instant.now();
        List<OwnerVerification> expiredList = ownerVerificationRepository.findByVerificationStatusAndExpiresAtBefore(
                VerificationStatus.APPROVED, now);

        for (OwnerVerification ov : expiredList) {
            try {
                ov.setVerificationStatus(VerificationStatus.EXPIRED);
                ownerVerificationRepository.save(ov);

                outboxEventPublisher.persistEvent("OwnerVerification", ov.getId().toString(),
                        new VerificationExpiredEvent(ov.getId(), ov.getUserId(), now));

                trustScoreService.recalculateTrustScore(ov.getUserId(), "VERIFICATION_EXPIRED");
                log.info("Verification ID: {} expired for user: {}", ov.getId(), ov.getUserId());
            } catch (Exception e) {
                log.error("Failed to expire verification ID: {}", ov.getId(), e);
            }
        }
        log.info("Finished VerificationExpiryJob.");
    }
}
