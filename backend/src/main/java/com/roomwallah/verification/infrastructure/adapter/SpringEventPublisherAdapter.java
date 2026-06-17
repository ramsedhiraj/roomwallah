package com.roomwallah.verification.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.verification.domain.port.EventPublisherPort;
import com.roomwallah.verification.infrastructure.outbox.OutboxEvent;
import com.roomwallah.verification.infrastructure.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.lang.reflect.Method;

@Slf4j
@Component("verificationSpringEventPublisherAdapter")
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(Object event) {
        try {
            String eventType = event.getClass().getName();
            String payload = objectMapper.writeValueAsString(event);
            
            // Try to resolve aggregate ID from event properties via reflection
            String aggregateId = "SYSTEM";
            try {
                Method userIdMethod = event.getClass().getMethod("userId");
                Object userIdVal = userIdMethod.invoke(event);
                if (userIdVal != null) {
                    aggregateId = userIdVal.toString();
                }
            } catch (Exception e) {
                // Ignore, try alternative methods
                try {
                    Method reqIdMethod = event.getClass().getMethod("verificationRequestId");
                    Object reqIdVal = reqIdMethod.invoke(event);
                    if (reqIdVal != null) {
                        aggregateId = reqIdVal.toString();
                    }
                } catch (Exception ex) {
                    // Fallback to default
                }
            }

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType(event.getClass().getSimpleName());
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(payload);
            outboxEvent.setStatus("PENDING");
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);
            log.info("Persisted domain event to Outbox: {}, Aggregate: {}", outboxEvent.getAggregateType(), aggregateId);
        } catch (Exception e) {
            log.error("Failed to persist domain event to Outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Outbox persistence failure", e);
        }
    }
}
