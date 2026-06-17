package com.roomwallah.audit.scheduler;

import com.roomwallah.audit.service.AuditLogService;
import com.roomwallah.common.lock.DatabaseLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditIntegrityVerificationJob {

    private final AuditLogService auditLogService;
    private final DatabaseLockService databaseLockService;

    @Scheduled(fixedDelayString = "${roomwallah.audit.verification-delay-ms:300000}") // Default 5 minutes
    public void run() {
        log.debug("Starting AuditIntegrityVerificationJob...");
        String lockName = "audit_integrity_verification_job";
        if (databaseLockService.acquireLock(lockName, 60)) {
            try {
                boolean result = auditLogService.verifyLogChain();
                if (!result) {
                    log.error("AUDIT CHAIN INTEGRITY CORRUPTED: Direct database manipulation detected!");
                } else {
                    log.info("Audit chain integrity verified successfully.");
                }
            } catch (Exception e) {
                log.error("Failed to run audit integrity verification job", e);
            } finally {
                databaseLockService.releaseLock(lockName);
            }
        } else {
            log.debug("AuditIntegrityVerificationJob lock already held. Skipping execution.");
        }
    }
}
