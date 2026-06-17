package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.trust.application.service.OutboxEventPublisher;
import com.roomwallah.trust.domain.event.VerificationApprovedEvent;
import com.roomwallah.trust.infrastructure.outbox.OutboxEvent;
import com.roomwallah.trust.infrastructure.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrustOutboxPatternTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ObjectMapper objectMapper;

    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // Since ObjectMapper needs standard java 8 time modules in production, we instantiate a raw one for basic tests
        outboxEventPublisher = new OutboxEventPublisher(
                outboxEventRepository,
                applicationEventPublisher,
                objectMapper
        );
    }

    @Test
    public void testPersistEvent_SavesEventInPendingState() throws Exception {
        UUID verificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VerificationApprovedEvent domainEvent = new VerificationApprovedEvent(verificationId, userId, Instant.now());

        // Execute
        outboxEventPublisher.persistEvent("OwnerVerification", verificationId.toString(), domainEvent);

        // Verify save is called
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        
        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertEquals("OwnerVerification", savedEvent.getAggregateType());
        assertEquals(verificationId.toString(), savedEvent.getAggregateId());
        assertEquals("PENDING", savedEvent.getStatus());
        assertEquals(VerificationApprovedEvent.class.getName(), savedEvent.getEventType());
        assertTrue(savedEvent.getPayload().contains(userId.toString()));
    }

    @Test
    public void testPublishPendingEvents_PublishesAndTransitionsToPublished() throws Exception {
        UUID verificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        // Setup mock event
        VerificationApprovedEvent domainEvent = new VerificationApprovedEvent(verificationId, userId, Instant.now());
        String payload = objectMapper.writeValueAsString(domainEvent);

        OutboxEvent pendingEvent = new OutboxEvent();
        pendingEvent.setId(UUID.randomUUID());
        pendingEvent.setAggregateType("OwnerVerification");
        pendingEvent.setAggregateId(verificationId.toString());
        pendingEvent.setEventType(VerificationApprovedEvent.class.getName());
        pendingEvent.setPayload(payload);
        pendingEvent.setStatus("PENDING");

        List<OutboxEvent> pendingList = new ArrayList<>();
        pendingList.add(pendingEvent);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(pendingList);

        // Execute
        outboxEventPublisher.publishPendingEvents();

        // Verify local application event published
        verify(applicationEventPublisher).publishEvent(any(VerificationApprovedEvent.class));

        // Verify status transitioned to PUBLISHED
        assertEquals("PUBLISHED", pendingEvent.getStatus());
        assertNotNull(pendingEvent.getProcessedAt());
        verify(outboxEventRepository).save(pendingEvent);
    }

    @Test
    public void testPublishPendingEvents_TransitionsToFailed_OnException() throws Exception {
        OutboxEvent corruptEvent = new OutboxEvent();
        corruptEvent.setId(UUID.randomUUID());
        corruptEvent.setEventType("com.roomwallah.NonExistentEventClass");
        corruptEvent.setPayload("{broken_json}");
        corruptEvent.setStatus("PENDING");

        List<OutboxEvent> pendingList = new ArrayList<>();
        pendingList.add(corruptEvent);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(pendingList);

        // Execute
        outboxEventPublisher.publishPendingEvents();

        // Verify no local publish happens
        verify(applicationEventPublisher, never()).publishEvent(any());

        // Verify status transitioned to FAILED
        assertEquals("FAILED", corruptEvent.getStatus());
        verify(outboxEventRepository).save(corruptEvent);
    }
}
