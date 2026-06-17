package com.roomwallah.verification.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.verification.infrastructure.outbox.OutboxEvent;
import com.roomwallah.verification.infrastructure.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.lang.reflect.Method;

@Slf4j
@Component("verificationOutboxPublisherJob")
@RequiredArgsConstructor
public class OutboxPublisherJob {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${roomwallah.outbox.publisher.delay:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Class<?> eventClass = Class.forName(event.getEventType());
                Object domainEvent = objectMapper.readValue(event.getPayload(), eventClass);

                // Extract and propagate correlation ID if present in the domain event
                String correlationId = null;
                try {
                    Method corrIdMethod = domainEvent.getClass().getMethod("correlationId");
                    Object corrIdVal = corrIdMethod.invoke(domainEvent);
                    if (corrIdVal != null) {
                        correlationId = corrIdVal.toString();
                    }
                } catch (Exception ex) {
                    // Ignore, event may not have a correlationId method
                }

                // Inject correlation ID into ThreadLocal log MDC context for downstream listeners
                CorrelationContext.set(correlationId);

                // Publish to Spring local event loop
                applicationEventPublisher.publishEvent(domainEvent);

                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Successfully published outbox event ID: {} type: {}", event.getId(), event.getAggregateType());
            } catch (Exception e) {
                log.error("Failed to process outbox event ID: {} due to: {}", event.getId(), e.getMessage(), e);
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            } finally {
                CorrelationContext.clear();
            }
        }
    }
}
