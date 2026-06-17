package com.roomwallah.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.application.service.BookingServiceImpl;
import com.roomwallah.booking.application.service.VisitServiceImpl;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingStatus;
import com.roomwallah.booking.domain.entity.SlotStatus;
import com.roomwallah.booking.domain.entity.VisitSlot;
import com.roomwallah.booking.domain.repository.BookingHistoryRepository;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.booking.domain.repository.VisitSlotRepository;
import com.roomwallah.booking.domain.repository.WaitlistEntryRepository;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConflictDetectionTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingHistoryRepository bookingHistoryRepository;

    @Mock
    private BookingOutboxRepository bookingOutboxRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private VisitSlotRepository visitSlotRepository;

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @InjectMocks
    private VisitServiceImpl visitService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testBookingConflict_OverlappingPendingRequests() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingRequestDto request = BookingRequestDto.builder()
                .propertyId(propertyId)
                .priceAmount(BigDecimal.valueOf(25000))
                .priceCurrency("INR")
                .idempotencyKey("idemp-key-conflict")
                .build();

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(ownerId);
        property.setStatus(PropertyStatus.ACTIVE);

        // Pre-existing pending booking request
        Booking preExisting = new Booking();
        preExisting.setPropertyId(propertyId);
        preExisting.setTenantId(tenantId);
        preExisting.setStatus(BookingStatus.PENDING);

        ArrayList<Booking> existingBookingsList = new ArrayList<>();
        existingBookingsList.add(preExisting);

        when(bookingRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(bookingRepository.findByTenantId(tenantId)).thenReturn(existingBookingsList);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(tenantId, request);
        });

        assertTrue(exception.getMessage().contains("active or pending booking"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void testVisitSlotOversubscription_FullSlot() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        PropertyVisitRequestDto request = PropertyVisitRequestDto.builder()
                .propertyId(propertyId)
                .visitSlotId(slotId)
                .build();

        VisitSlot fullSlot = new VisitSlot();
        fullSlot.setId(slotId);
        fullSlot.setPropertyId(propertyId);
        fullSlot.setMaxBookings(3);
        fullSlot.setCurrentBookings(3); // Already fully booked
        fullSlot.setStatus(SlotStatus.BOOKED);

        when(visitSlotRepository.findAndLockById(slotId)).thenReturn(Optional.of(fullSlot));
        when(waitlistEntryRepository.findByVisitSlotIdOrderByPriorityAsc(slotId)).thenReturn(new ArrayList<>());

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            visitService.scheduleVisit(tenantId, request);
        });

        assertTrue(exception.getMessage().contains("Visit slot is full"));
        verify(waitlistEntryRepository, times(1)).save(any());
    }
}
