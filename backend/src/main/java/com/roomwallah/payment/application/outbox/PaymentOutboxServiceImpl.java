package com.roomwallah.payment.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxServiceImpl implements PaymentOutboxService {

    private final PaymentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void persistEvent(String aggregateType, String aggregateId, Object domainEvent) {
        try {
            String payload = objectMapper.writeValueAsString(domainEvent);
            PaymentOutboxEvent event = new PaymentOutboxEvent();
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(domainEvent.getClass().getName());
            event.setPayload(payload);
            event.setStatus("PENDING");
            event.setCreatedAt(Instant.now());
            
            outboxRepository.save(event);
            log.info("PaymentOutboxService: Persisted outbox event for {} aggregate ID: {}", aggregateType, aggregateId);
        } catch (Exception e) {
            log.error("PaymentOutboxService: Failed to persist outbox event", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }
}
