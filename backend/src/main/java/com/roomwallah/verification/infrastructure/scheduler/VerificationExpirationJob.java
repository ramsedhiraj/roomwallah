package com.roomwallah.verification.infrastructure.scheduler;

import com.roomwallah.verification.application.service.VerificationReviewService;
import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.entity.VerificationRequestStatus;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class VerificationExpirationJob {

    private final VerificationRequestRepository requestRepository;
    private final VerificationReviewService reviewService;
    private final Clock clock;
    
    // System UUID for automated scheduler executions
    private static final UUID SYSTEM_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public VerificationExpirationJob(VerificationRequestRepository requestRepository, VerificationReviewService reviewService, Clock clock) {
        this.requestRepository = requestRepository;
        this.reviewService = reviewService;
        this.clock = clock;
    }

    @Scheduled(cron = "${roomwallah.verification.expiration-check.cron:0 0 0 * * *}") // Defaults to daily at midnight
    @Transactional
    public void scanAndExpireVerifications() {
        log.info("Scanning for expired verifications...");
        Instant now = Instant.now(clock);
        
        // Find requests that are currently VERIFIED and have expired
        List<VerificationRequest> expiredRequests = requestRepository
                .findByRequestStatusAndExpiresAtBefore(VerificationRequestStatus.VERIFIED, now);

        if (expiredRequests.isEmpty()) {
            log.info("No expired verifications found.");
            return;
        }

        log.info("Found {} verifications that have expired. Processing transition to EXPIRED...", expiredRequests.size());
        for (VerificationRequest req : expiredRequests) {
            try {
                reviewService.expire(req.getId(), SYSTEM_ADMIN_ID, "Verification validity period has elapsed and expired automatically.");
                log.info("Request {} has been marked as EXPIRED automatically.", req.getId());
            } catch (Exception e) {
                log.error("Failed to expire request ID {}: {}", req.getId(), e.getMessage(), e);
            }
        }
        log.info("Completed automatic verification expiration check.");
    }
}
