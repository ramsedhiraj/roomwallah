package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.application.service.OutboxEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrustOutboxPublisherJob {

    private final OutboxEventPublisher outboxEventPublisher;

    public TrustOutboxPublisherJob(OutboxEventPublisher outboxEventPublisher) {
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Scheduled(fixedDelayString = "${roomwallah.trust.outbox.publisher.delay:1000}")
    public void run() {
        try {
            outboxEventPublisher.publishPendingEvents();
        } catch (Exception e) {
            log.error("Outbox publisher failed to run", e);
        }
    }
}
