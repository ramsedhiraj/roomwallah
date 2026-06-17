package com.roomwallah.trust.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.trust.infrastructure.outbox.OutboxEvent;
import com.roomwallah.trust.infrastructure.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("OutboxEventPublisher: Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Class<?> eventClass = Class.forName(event.getEventType());
                Object domainEvent = objectMapper.readValue(event.getPayload(), eventClass);

                // Publish locally
                applicationEventPublisher.publishEvent(domainEvent);

                event.setStatus("PUBLISHED");
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("OutboxEventPublisher: Successfully published event ID: {} of type: {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("OutboxEventPublisher: Failed to process event ID: {} due to: {}", event.getId(), e.getMessage(), e);
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            }
        }
    }

    @Transactional
    public void persistEvent(String aggregateType, String aggregateId, Object domainEvent) {
        try {
            String payload = objectMapper.writeValueAsString(domainEvent);
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setEventType(domainEvent.getClass().getName());
            outboxEvent.setPayload(payload);
            outboxEvent.setStatus("PENDING");
            outboxEventRepository.save(outboxEvent);
            log.info("OutboxEventPublisher: Persisted event for aggregate ID: {} of type: {}", aggregateId, domainEvent.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("OutboxEventPublisher: Failed to serialize and persist event", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }
}
