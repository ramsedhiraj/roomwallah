package com.roomwallah.booking.infrastructure.scheduler;

import com.roomwallah.booking.application.service.BookingOutboxPublisher;
import com.roomwallah.booking.application.service.BookingService;
import com.roomwallah.booking.application.service.ReminderService;
import com.roomwallah.booking.config.BookingProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingJobs {

    private final BookingService bookingService;
    private final ReminderService reminderService;
    private final BookingOutboxPublisher bookingOutboxPublisher;
    private final BookingProperties bookingProperties;
    private final EntityManager entityManager;

    private static final long EXPIRY_JOB_LOCK_ID = 88001L;
    private static final long REMINDER_JOB_LOCK_ID = 88002L;
    private static final long OUTBOX_JOB_LOCK_ID = 88003L;

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void runBookingExpiryJob() {
        if (!bookingProperties.getFeatures().isExpiryEnabled()) {
            log.debug("Booking expiry job is disabled via feature flags.");
            return;
        }
        executeWithLock(EXPIRY_JOB_LOCK_ID, () -> {
            log.info("Starting BookingExpiryJob with distributed advisory lock");
            bookingService.expireUnconfirmedBookings();
            log.info("Completed BookingExpiryJob");
        });
    }

    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    @Transactional
    public void runReminderDispatchJob() {
        if (!bookingProperties.getFeatures().isRemindersEnabled()) {
            log.debug("Booking reminder job is disabled via feature flags.");
            return;
        }
        executeWithLock(REMINDER_JOB_LOCK_ID, () -> {
            log.debug("Starting ReminderDispatchJob with distributed advisory lock");
            reminderService.dispatchReminders();
            log.debug("Completed ReminderDispatchJob");
        });
    }

    @Scheduled(fixedDelay = 10000) // Every 10 seconds
    @Transactional
    public void runOutboxPollerJob() {
        executeWithLock(OUTBOX_JOB_LOCK_ID, () -> {
            log.debug("Starting OutboxPollerJob with distributed advisory lock");
            bookingOutboxPublisher.publishEvents();
            log.debug("Completed OutboxPollerJob");
        });
    }

    private void executeWithLock(long lockId, Runnable task) {
        try {
            // Check if we can acquire the advisory lock
            Boolean locked = (Boolean) entityManager.createNativeQuery("SELECT pg_try_advisory_lock(:lockId)")
                    .setParameter("lockId", lockId)
                    .getSingleResult();

            if (Boolean.TRUE.equals(locked)) {
                try {
                    task.run();
                } finally {
                    // Always release the lock
                    entityManager.createNativeQuery("SELECT pg_advisory_unlock(:lockId)")
                            .setParameter("lockId", lockId)
                            .getSingleResult();
                }
            } else {
                log.debug("Advisory lock {} already held by another instance. Skipping job execution.", lockId);
            }
        } catch (Exception e) {
            log.error("Failed to execute job with advisory lock: {}", lockId, e);
        }
    }
}
