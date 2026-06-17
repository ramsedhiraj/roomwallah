package com.roomwallah.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.application.service.BookingServiceImpl;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingStatus;
import com.roomwallah.booking.domain.repository.BookingHistoryRepository;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingHistoryRepository bookingHistoryRepository;

    @Mock
    private BookingOutboxRepository bookingOutboxRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateBooking_Success() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingRequestDto request = BookingRequestDto.builder()
                .propertyId(propertyId)
                .priceAmount(BigDecimal.valueOf(15000))
                .priceCurrency("INR")
                .notes("Requesting early move-in")
                .idempotencyKey("unique-key-123")
                .build();

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(ownerId);
        property.setStatus(PropertyStatus.ACTIVE);

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setPropertyId(propertyId);
        booking.setTenantId(tenantId);
        booking.setOwnerId(ownerId);
        booking.setPriceAmount(request.getPriceAmount());
        booking.setPriceCurrency(request.getPriceCurrency());
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findByIdempotencyKey("unique-key-123")).thenReturn(Optional.empty());
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(bookingRepository.findByTenantId(tenantId)).thenReturn(new ArrayList<>());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Booking result = bookingService.createBooking(tenantId, request);

        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(bookingOutboxRepository, times(1)).save(any());
        verify(bookingHistoryRepository, times(1)).save(any());
    }

    @Test
    public void testCreateBooking_IdempotencyTriggered() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        BookingRequestDto request = BookingRequestDto.builder()
                .propertyId(propertyId)
                .priceAmount(BigDecimal.valueOf(15000))
                .priceCurrency("INR")
                .idempotencyKey("unique-key-123")
                .build();

        Booking existingBooking = new Booking();
        existingBooking.setId(UUID.randomUUID());
        existingBooking.setPropertyId(propertyId);
        existingBooking.setTenantId(tenantId);
        existingBooking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findByIdempotencyKey("unique-key-123")).thenReturn(Optional.of(existingBooking));

        Booking result = bookingService.createBooking(tenantId, request);

        assertNotNull(result);
        assertEquals(existingBooking.getId(), result.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    public void testCreateBooking_ConflictDetected() {
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingRequestDto request = BookingRequestDto.builder()
                .propertyId(propertyId)
                .priceAmount(BigDecimal.valueOf(15000))
                .priceCurrency("INR")
                .idempotencyKey("unique-key-123")
                .build();

        Property property = new Property();
        property.setId(propertyId);
        property.setOwnerId(ownerId);
        property.setStatus(PropertyStatus.ACTIVE);

        Booking existingActiveBooking = new Booking();
        existingActiveBooking.setPropertyId(propertyId);
        existingActiveBooking.setTenantId(tenantId);
        existingActiveBooking.setStatus(BookingStatus.CONFIRMED);

        ArrayList<Booking> existingList = new ArrayList<>();
        existingList.add(existingActiveBooking);

        when(bookingRepository.findByIdempotencyKey("unique-key-123")).thenReturn(Optional.empty());
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(bookingRepository.findByTenantId(tenantId)).thenReturn(existingList);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(tenantId, request);
        });

        assertTrue(exception.getMessage().contains("active or pending booking"));
    }
}
