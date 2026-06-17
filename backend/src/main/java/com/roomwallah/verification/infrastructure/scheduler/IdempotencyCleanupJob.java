package com.roomwallah.verification.infrastructure.scheduler;

import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class IdempotencyCleanupJob {

    private final VerificationRequestRepository requestRepository;
    private final Clock clock;

    public IdempotencyCleanupJob(VerificationRequestRepository requestRepository, Clock clock) {
        this.requestRepository = requestRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${roomwallah.verification.idempotency-cleanup.cron:0 0 * * * *}") // Defaults to hourly
    @Transactional
    public void cleanupExpiredIdempotencyKeys() {
        log.info("Running idempotency key cleanup job...");
        Instant now = Instant.now(clock);
        List<VerificationRequest> expiredRequests = requestRepository
                .findByIdempotencyCleanupAfterBeforeAndIdempotencyKeyIsNotNull(now);

        if (expiredRequests.isEmpty()) {
            log.info("No expired idempotency keys found.");
            return;
        }

        log.info("Found {} verification requests with expired idempotency keys for cleanup.", expiredRequests.size());
        for (VerificationRequest req : expiredRequests) {
            req.setIdempotencyKey(null);
            req.setIdempotencyExpiresAt(null);
            req.setIdempotencyCleanupAfter(null);
            requestRepository.save(req);
        }
        log.info("Successfully cleared {} expired idempotency keys.", expiredRequests.size());
    }
}
