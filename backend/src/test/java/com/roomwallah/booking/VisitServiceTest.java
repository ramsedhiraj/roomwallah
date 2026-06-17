package com.roomwallah.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.application.service.VisitServiceImpl;
import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.SlotStatus;
import com.roomwallah.booking.domain.entity.VisitSlot;
import com.roomwallah.booking.domain.entity.VisitStatus;
import com.roomwallah.booking.domain.entity.WaitlistEntry;
import com.roomwallah.booking.domain.port.LeadScoringPort;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import com.roomwallah.booking.domain.repository.BookingReminderRepository;
import com.roomwallah.booking.domain.repository.LeadRepository;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.booking.domain.repository.VisitSlotRepository;
import com.roomwallah.booking.domain.repository.WaitlistEntryRepository;
import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VisitServiceTest {

    @Mock
    private VisitSlotRepository visitSlotRepository;

    @Mock
    private PropertyVisitRepository propertyVisitRepository;

    @Mock
    private BookingReminderRepository bookingReminderRepository;

    @Mock
    private BookingOutboxRepository bookingOutboxRepository;

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadScoringPort leadScoringPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VisitServiceImpl visitService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testScheduleVisit_Success() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        PropertyVisitRequestDto request = PropertyVisitRequestDto.builder()
                .propertyId(propertyId)
                .visitSlotId(slotId)
                .notes("Need parking space details")
                .build();

        VisitSlot slot = new VisitSlot();
        slot.setId(slotId);
        slot.setPropertyId(propertyId);
        slot.setStartTime(Instant.now().plusSeconds(3600));
        slot.setEndTime(Instant.now().plusSeconds(7200));
        slot.setMaxBookings(2);
        slot.setCurrentBookings(0);
        slot.setStatus(SlotStatus.AVAILABLE);

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(UUID.randomUUID());

        PropertyVisit visit = new PropertyVisit();
        visit.setId(UUID.randomUUID());
        visit.setPropertyId(propertyId);
        visit.setTenantId(tenantId);
        visit.setVisitSlotId(slotId);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setStartTime(slot.getStartTime());
        visit.setEndTime(slot.getEndTime());

        when(visitSlotRepository.findAndLockById(slotId)).thenReturn(Optional.of(slot));
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyVisitRepository.save(any(PropertyVisit.class))).thenReturn(visit);
        when(leadRepository.findByPropertyIdAndTenantId(propertyId, tenantId)).thenReturn(Optional.empty());
        when(leadScoringPort.calculateLeadScore(any(), any())).thenReturn(new LeadScoreExplanation(85, "Verified Profile"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        PropertyVisit result = visitService.scheduleVisit(tenantId, request);

        assertNotNull(result);
        assertEquals(VisitStatus.SCHEDULED, result.getStatus());
        verify(visitSlotRepository, times(1)).save(slot);
        verify(bookingReminderRepository, times(1)).save(any());
        verify(leadRepository, times(1)).save(any());
        verify(bookingOutboxRepository, times(1)).save(any());
    }

    @Test
    public void testScheduleVisit_SlotFull_WaitlistAdded() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        PropertyVisitRequestDto request = PropertyVisitRequestDto.builder()
                .propertyId(propertyId)
                .visitSlotId(slotId)
                .build();

        VisitSlot slot = new VisitSlot();
        slot.setId(slotId);
        slot.setPropertyId(propertyId);
        slot.setMaxBookings(1);
        slot.setCurrentBookings(1);
        slot.setStatus(SlotStatus.BOOKED);

        when(visitSlotRepository.findAndLockById(slotId)).thenReturn(Optional.of(slot));
        when(waitlistEntryRepository.findByVisitSlotIdOrderByPriorityAsc(slotId)).thenReturn(new ArrayList<>());

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            visitService.scheduleVisit(tenantId, request);
        });

        assertTrue(exception.getMessage().contains("waitlist"));
        verify(waitlistEntryRepository, times(1)).save(any(WaitlistEntry.class));
        verify(propertyVisitRepository, never()).save(any());
    }
}
