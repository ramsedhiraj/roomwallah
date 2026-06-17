package com.roomwallah.payment.infrastructure.scheduler;

import com.roomwallah.payment.application.service.EscrowService;
import com.roomwallah.payment.application.service.ReconciliationService;
import com.roomwallah.payment.application.service.WebhookService;
import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.EscrowStatus;
import com.roomwallah.payment.domain.entity.PaymentWebhook;
import com.roomwallah.payment.domain.port.DisputeRepositoryPort;
import com.roomwallah.payment.domain.port.EscrowRepositoryPort;
import com.roomwallah.payment.domain.port.WebhookRepositoryPort;
import com.roomwallah.payment.infrastructure.config.PaymentProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Scheduled background job coordinator for the Payment bounded context.
 *
 * <p>All jobs acquire a PostgreSQL advisory lock before executing to prevent
 * duplicate processing in a multi-node deployment. Advisory locks are automatically
 * released at the end of the transaction.
 *
 * <p>Lock IDs are chosen in a dedicated numeric namespace (99001–99010) to avoid
 * collisions with other bounded contexts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentJobs {

    private final EscrowRepositoryPort escrowRepositoryPort;
    private final WebhookRepositoryPort webhookRepositoryPort;
    private final DisputeRepositoryPort disputeRepositoryPort;
    private final EscrowService escrowService;
    private final WebhookService webhookService;
    private final ReconciliationService reconciliationService;
    private final PaymentProperties paymentProperties;
    private final EntityManager entityManager;

    // Advisory lock IDs — must be unique across the entire application
    private static final long RECONCILIATION_LOCK_ID = 99001L;
    private static final long ESCROW_RELEASE_LOCK_ID = 99002L;
    private static final long DISPUTE_SWEEP_LOCK_ID  = 99003L;
    private static final long WEBHOOK_RETRY_LOCK_ID  = 99004L;

    /** Escrow accounts older than this many days are eligible for automatic release. */
    private static final long ESCROW_HOLD_DAYS = 30L;

    // ─────────────────────────────────────────────────────────────────────────
    // Nightly reconciliation — 02:00 every night
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runNightlyReconciliation() {
        if (!paymentProperties.isPaymentsEnabled()) {
            log.debug("[PaymentJobs] Payments disabled — skipping nightly reconciliation.");
            return;
        }
        executeWithLock(RECONCILIATION_LOCK_ID, () -> {
            log.info("[PaymentJobs] Starting nightly gateway reconciliation for provider={}",
                    paymentProperties.getGateway().getActiveProvider());
            // In a full implementation, fetch today's gateway records via the gateway client
            // and hand them to the reconciliation service. We pass an empty list here as the
            // actual gateway data retrieval depends on provider-specific API calls.
            reconciliationService.reconcileGatewayTransactions(
                    paymentProperties.getGateway().getActiveProvider(),
                    Collections.emptyList()
            );
            log.info("[PaymentJobs] Nightly gateway reconciliation completed.");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escrow release sweep — every 4 hours
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void runEscrowReleaseSweep() {
        if (!paymentProperties.isEscrowEnabled()) {
            log.debug("[PaymentJobs] Escrow disabled — skipping release sweep.");
            return;
        }
        executeWithLock(ESCROW_RELEASE_LOCK_ID, () -> {
            log.info("[PaymentJobs] Starting escrow release sweep (held > {} days)", ESCROW_HOLD_DAYS);

            Instant cutoff = Instant.now().minus(ESCROW_HOLD_DAYS, ChronoUnit.DAYS);
            List<EscrowAccount> heldAccounts = escrowRepositoryPort.findByStatus(EscrowStatus.HELD);

            int released = 0;
            int skipped  = 0;
            for (EscrowAccount account : heldAccounts) {
                if (account.getHeldAt() != null && account.getHeldAt().isBefore(cutoff)) {
                    try {
                        escrowService.releaseFunds(account.getId());
                        released++;
                        log.info("[PaymentJobs] Released escrow account={} (heldAt={})",
                                account.getId(), account.getHeldAt());
                    } catch (Exception e) {
                        log.error("[PaymentJobs] Failed to release escrow account={}", account.getId(), e);
                        skipped++;
                    }
                } else {
                    skipped++;
                }
            }
            log.info("[PaymentJobs] Escrow release sweep completed: released={}, skipped={}", released, skipped);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dispute sweep — every day at 09:00
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void runDisputeSweep() {
        if (!paymentProperties.isDisputesEnabled()) {
            log.debug("[PaymentJobs] Disputes disabled — skipping dispute sweep.");
            return;
        }
        executeWithLock(DISPUTE_SWEEP_LOCK_ID, () -> {
            log.info("[PaymentJobs] Starting daily dispute sweep");
            long openDisputeCount = disputeRepositoryPort.countOpen();
            log.info("[PaymentJobs] Dispute sweep completed — open disputes: {}", openDisputeCount);
            // Production: escalate aged disputes, send alert emails, update statuses
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Webhook retry — every 5 minutes
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void runWebhookRetry() {
        executeWithLock(WEBHOOK_RETRY_LOCK_ID, () -> {
            List<PaymentWebhook> unprocessed = webhookRepositoryPort.findByProcessed(false);
            if (unprocessed.isEmpty()) {
                log.debug("[PaymentJobs] No unprocessed webhooks found.");
                return;
            }
            log.info("[PaymentJobs] Retrying {} unprocessed webhook(s)", unprocessed.size());
            int success = 0;
            int failed  = 0;
            for (PaymentWebhook webhook : unprocessed) {
                try {
                    webhookService.processWebhook(
                            webhook.getGatewayProvider(),
                            webhook.getEventType(),
                            webhook.getPayloadJson()
                    );
                    success++;
                } catch (Exception e) {
                    log.error("[PaymentJobs] Webhook retry failed for webhookId={}", webhook.getId(), e);
                    failed++;
                }
            }
            log.info("[PaymentJobs] Webhook retry completed: success={}, failed={}", success, failed);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Advisory lock helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tries to acquire a PostgreSQL session-level advisory lock before running {@code task}.
     * The lock is released unconditionally in the {@code finally} block.
     * If another node already holds the lock the task is silently skipped.
     */
    private void executeWithLock(long lockId, Runnable task) {
        try {
            Boolean locked = (Boolean) entityManager
                    .createNativeQuery("SELECT pg_try_advisory_lock(:lockId)")
                    .setParameter("lockId", lockId)
                    .getSingleResult();

            if (Boolean.TRUE.equals(locked)) {
                try {
                    task.run();
                } finally {
                    entityManager
                            .createNativeQuery("SELECT pg_advisory_unlock(:lockId)")
                            .setParameter("lockId", lockId)
                            .getSingleResult();
                }
            } else {
                log.debug("[PaymentJobs] Advisory lock {} already held by another node. Skipping.", lockId);
            }
        } catch (Exception e) {
            log.error("[PaymentJobs] Error acquiring advisory lock {} or running task", lockId, e);
        }
    }
}
