package com.roomwallah.notification.scheduler;

import com.roomwallah.common.lock.DatabaseLockService;
import com.roomwallah.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final NotificationService notificationService;
    private final DatabaseLockService databaseLockService;

    @Scheduled(fixedDelayString = "${roomwallah.notification.retry-delay-ms:10000}") // Default 10 seconds
    public void run() {
        log.debug("Starting NotificationRetryJob...");
        String lockName = "notification_retry_job";
        if (databaseLockService.acquireLock(lockName, 60)) {
            try {
                notificationService.processRetryQueue();
            } catch (Exception e) {
                log.error("Failed to run notification retry queue processing", e);
            } finally {
                databaseLockService.releaseLock(lockName);
            }
        } else {
            log.debug("NotificationRetryJob lock already held. Skipping execution.");
        }
    }
}
