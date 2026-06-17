package com.roomwallah.partner.scheduler;

import com.roomwallah.common.lock.DatabaseLockService;
import com.roomwallah.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyExpirationWarningJob {

    private final PartnerService partnerService;
    private final DatabaseLockService databaseLockService;

    @Scheduled(cron = "${roomwallah.partner.expiry-warning-cron:0 0 4 * * ?}") // 4:00 AM daily
    public void run() {
        log.info("Starting ApiKeyExpirationWarningJob...");
        String lockName = "partner_expiry_warning_job";
        if (databaseLockService.acquireLock(lockName, 300)) {
            try {
                partnerService.checkKeyExpirations();
            } catch (Exception e) {
                log.error("Failed to execute ApiKeyExpirationWarningJob", e);
            } finally {
                databaseLockService.releaseLock(lockName);
            }
        } else {
            log.info("ApiKeyExpirationWarningJob lock already held. Skipping execution.");
        }
    }
}
