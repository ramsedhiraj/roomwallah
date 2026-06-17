package com.roomwallah.analytics.scheduler;

import com.roomwallah.analytics.service.AnalyticsService;
import com.roomwallah.common.lock.DatabaseLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsNightlyJob {

    private final AnalyticsService analyticsService;
    private final DatabaseLockService databaseLockService;

    @Scheduled(cron = "${roomwallah.analytics.nightly-cron:0 0 1 * * ?}") // Daily at 1:00 AM
    public void runDailyAggregation() {
        log.info("Starting Daily Aggregation Job...");
        String lockName = "analytics_daily_job";
        if (databaseLockService.acquireLock(lockName, 600)) {
            try {
                LocalDate yesterday = LocalDate.now().minusDays(1);
                analyticsService.performDailyAggregation(yesterday);
                log.info("Finished Daily Aggregation Job successfully for {}", yesterday);
            } catch (Exception e) {
                log.error("Failed to execute Daily Aggregation Job", e);
            } finally {
                databaseLockService.releaseLock(lockName);
            }
        } else {
            log.info("Daily Aggregation lock already held. Skipping execution.");
        }
    }

    @Scheduled(cron = "${roomwallah.analytics.hourly-cron:0 0 * * * ?}") // Hourly
    public void runHourlyAggregation() {
        log.info("Starting Hourly Aggregation Job...");
        String lockName = "analytics_hourly_job";
        if (databaseLockService.acquireLock(lockName, 120)) {
            try {
                LocalDateTime now = LocalDateTime.now().minusHours(1);
                analyticsService.performHourlyAggregation(now.toLocalDate(), now.getHour());
                log.info("Finished Hourly Aggregation Job successfully for {} hour {}", now.toLocalDate(), now.getHour());
            } catch (Exception e) {
                log.error("Failed to execute Hourly Aggregation Job", e);
            } finally {
                databaseLockService.releaseLock(lockName);
            }
        } else {
            log.info("Hourly Aggregation lock already held. Skipping execution.");
        }
    }
}
