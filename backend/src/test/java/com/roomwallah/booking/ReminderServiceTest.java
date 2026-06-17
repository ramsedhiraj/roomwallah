package com.roomwallah.booking;

import com.roomwallah.booking.application.service.ReminderServiceImpl;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingReminder;
import com.roomwallah.booking.domain.entity.ReminderType;
import com.roomwallah.booking.domain.repository.BookingReminderRepository;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReminderServiceTest {

    @Mock
    private BookingReminderRepository bookingReminderRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyVisitRepository propertyVisitRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private ReminderServiceImpl reminderService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDispatchReminders_Success() {
        UUID reminderId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        BookingReminder reminder = new BookingReminder();
        reminder.setId(reminderId);
        reminder.setBookingId(bookingId);
        reminder.setStatus("PENDING");
        reminder.setType(ReminderType.EMAIL);
        reminder.setTriggerAt(Instant.now().minusSeconds(10));

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTenantId(tenantId);
        booking.setPropertyId(propertyId);

        User tenant = new User();
        tenant.setId(tenantId);
        tenant.setFullName("Rohan Sharma");
        tenant.setEmail("rohan@example.com");

        Property property = new Property();
        property.setId(propertyId);
        property.setTitle("Sleek 2BHK in Bangalore");

        ArrayList<BookingReminder> list = new ArrayList<>();
        list.add(reminder);

        when(bookingReminderRepository.findByStatusAndTriggerAtBefore(eq("PENDING"), any())).thenReturn(list);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        reminderService.dispatchReminders();

        verify(notificationPort, times(1)).sendEmail(eq("rohan@example.com"), any(), any());
        assertEquals("DISPATCHED", reminder.getStatus());
        verify(bookingReminderRepository, times(1)).save(reminder);
    }

    @Test
    public void testDispatchReminders_ExponentialBackoff() {
        UUID reminderId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        BookingReminder reminder = new BookingReminder();
        reminder.setId(reminderId);
        reminder.setBookingId(bookingId);
        reminder.setStatus("PENDING");
        reminder.setType(ReminderType.EMAIL);
        reminder.setTriggerAt(Instant.now().minusSeconds(10));
        reminder.setRetryCount(1); // will become 2

        ArrayList<BookingReminder> list = new ArrayList<>();
        list.add(reminder);

        when(bookingReminderRepository.findByStatusAndTriggerAtBefore(eq("PENDING"), any())).thenReturn(list);
        when(bookingRepository.findById(bookingId)).thenThrow(new RuntimeException("Database timeout"));

        reminderService.dispatchReminders();

        assertEquals("PENDING", reminder.getStatus());
        assertEquals(2, reminder.getRetryCount());
        assertTrue(reminder.getTriggerAt().isAfter(Instant.now().plusSeconds(200))); // Backoff is 2^2 = 4 mins
        verify(bookingReminderRepository, times(1)).save(reminder);
    }
}
