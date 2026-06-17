package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.Lead;
import com.roomwallah.booking.domain.event.LeadCreatedEvent;
import com.roomwallah.booking.domain.port.LeadScoringPort;
import com.roomwallah.booking.domain.repository.LeadRepository;
import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
public class LeadServiceIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private LeadRepository leadRepository;

    @MockBean
    private LeadScoringPort leadScoringPort;

    @Autowired
    private ApplicationEvents events;

    @Test
    public void testGetOrCreateLead_persistsLeadAndPublishesEvent() {
        UUID propertyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        // Mock the port to avoid external API calls during DB test
        LeadScoreExplanation explanation = mock(LeadScoreExplanation.class);
        when(explanation.getScore()).thenReturn(85);
        when(explanation.getExplanation()).thenReturn("High intent");
        when(leadScoringPort.calculateLeadScore(any(), any())).thenReturn(explanation);

        // Execute
        Lead lead = leadService.getOrCreateLead(propertyId, tenantId, ownerId, "I am very interested", "555-1234", "tenant@test.com");

        // Verify Object
        assertThat(lead).isNotNull();
        assertThat(lead.getId()).isNotNull();

        // Verify DB Row
        Lead savedLead = leadRepository.findById(lead.getId()).orElse(null);
        assertThat(savedLead).isNotNull();
        assertThat(savedLead.getInquiryText()).isEqualTo("I am very interested");
        assertThat(savedLead.getLeadScore()).isEqualTo(85);

        // Verify Event Published
        long eventCount = events.stream(LeadCreatedEvent.class)
                .filter(e -> e.getLeadId().equals(lead.getId()))
                .count();
        assertThat(eventCount).isEqualTo(1);
    }
}
