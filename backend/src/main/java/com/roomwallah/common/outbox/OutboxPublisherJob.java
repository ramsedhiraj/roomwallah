package com.roomwallah.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherJob {

    private final DomainOutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutbox() {
        Instant now = Instant.now();
        List<DomainOutboxEvent> pending = repository.findPendingEvents(now);
        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to process.", pending.size());
        for (DomainOutboxEvent event : pending) {
            try {
                publishEvent(event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                event.setErrorLog(null);
                log.info("Successfully published outbox event: {}", event.getId());
            } catch (Exception e) {
                int attempts = event.getRetryCount() + 1;
                event.setRetryCount(attempts);
                event.setErrorLog(e.getMessage());
                
                if (attempts >= 5) {
                    event.setStatus("DLQ");
                    log.error("Outbox event {} reached max retries. Moving to DLQ.", event.getId());
                } else {
                    // Exponential backoff: 2^attempts seconds
                    long backoffSec = (long) Math.pow(2, attempts);
                    event.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(backoffSec)));
                    log.warn("Failed to publish outbox event {}. Scheduling retry #{} in {} seconds.", 
                            event.getId(), attempts, backoffSec);
                }
            }
            repository.save(event);
        }
    }

    private void publishEvent(DomainOutboxEvent event) throws Exception {
        // Simulating outbox delivery to external message broker
        log.debug("Simulating publish to message broker: aggregateType={}, eventType={}", 
                event.getAggregateType(), event.getEventType());
        
        // Dynamic simulated failure for testing outbox retry handling
        if (event.getEventType().contains("SIMULATED_FAIL")) {
            throw new RuntimeException("Simulated message broker connection error");
        }
    }

    @Transactional
    public void submitEvent(String aggregateType, String aggregateId, String eventType, Object payload, String idempotencyKey) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String finalKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            
            // Deduplication check
            if (repository.findByIdempotencyKey(finalKey).isPresent()) {
                log.info("Duplicate outbox event discarded: idempotencyKey={}", finalKey);
                return;
            }

            DomainOutboxEvent event = DomainOutboxEvent.builder()
                    .idempotencyKey(finalKey)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(jsonPayload)
                    .status("PENDING")
                    .retryCount(0)
                    .nextAttemptAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            repository.save(event);
            log.info("Saved domain outbox event: aggregateId={}, type={}", aggregateId, eventType);
        } catch (Exception e) {
            log.error("Failed to enqueue domain outbox event: aggregateId={}", aggregateId, e);
        }
    }
}
