package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingReminder;
import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.repository.BookingReminderRepository;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final BookingReminderRepository bookingReminderRepository;
    private final BookingRepository bookingRepository;
    private final PropertyVisitRepository propertyVisitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationPort notificationPort;

    @Override
    @Transactional
    public void dispatchReminders() {
        log.info("Starting reminder dispatch cycle");
        Instant now = Instant.now();
        List<BookingReminder> pendingReminders = bookingReminderRepository.findByStatusAndTriggerAtBefore("PENDING", now);

        for (BookingReminder reminder : pendingReminders) {
            try {
                processReminder(reminder);
            } catch (Exception e) {
                log.error("Failed to process reminder ID: {}", reminder.getId(), e);
                handleFailure(reminder, e.getMessage());
            }
        }
    }

    private void processReminder(BookingReminder reminder) {
        if (reminder.getBookingId() != null) {
            sendBookingReminder(reminder);
        } else if (reminder.getVisitId() != null) {
            sendVisitReminder(reminder);
        } else {
            reminder.setStatus("SKIPPED");
            bookingReminderRepository.save(reminder);
        }
    }

    private void sendBookingReminder(BookingReminder reminder) {
        Booking booking = bookingRepository.findById(reminder.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + reminder.getBookingId()));

        User tenant = userRepository.findById(booking.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + booking.getTenantId()));

        Property property = propertyRepository.findById(booking.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + booking.getPropertyId()));

        String emailSubject = "RoomWallah - Booking Status Update Alert";
        String emailBody = String.format("Hello %s, this is an update regarding your booking status for property: %s. Current status: %s.",
                tenant.getFullName(), property.getTitle(), booking.getStatus());

        notificationPort.sendEmail(tenant.getEmail(), emailSubject, emailBody);

        reminder.setStatus("DISPATCHED");
        bookingReminderRepository.save(reminder);
        log.info("Booking reminder sent successfully for booking: {}", booking.getId());
    }

    private void sendVisitReminder(BookingReminder reminder) {
        PropertyVisit visit = propertyVisitRepository.findById(reminder.getVisitId())
                .orElseThrow(() -> new IllegalArgumentException("Property visit not found: " + reminder.getVisitId()));

        User tenant = userRepository.findById(visit.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + visit.getTenantId()));

        Property property = propertyRepository.findById(visit.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + visit.getPropertyId()));

        String emailSubject = "RoomWallah - Scheduled Visit Reminder";
        String emailBody = String.format("Hello %s, this is a reminder for your upcoming property visit for: %s scheduled on %s.",
                tenant.getFullName(), property.getTitle(), visit.getStartTime().toString());

        notificationPort.sendEmail(tenant.getEmail(), emailSubject, emailBody);

        reminder.setStatus("DISPATCHED");
        bookingReminderRepository.save(reminder);
        log.info("Visit reminder sent successfully for visit: {}", visit.getId());
    }

    private void handleFailure(BookingReminder reminder, String errorMessage) {
        reminder.setRetryCount(reminder.getRetryCount() + 1);
        reminder.setLastError(errorMessage);

        if (reminder.getRetryCount() >= 5) {
            reminder.setStatus("FAILED_EXHAUSTED");
            log.error("Reminder: {} exceeded maximum retries. Marked as FAILED_EXHAUSTED.", reminder.getId());
        } else {
            // Exponential backoff logic: retry after 2^retryCount minutes
            long backoffMinutes = (long) Math.pow(2, reminder.getRetryCount());
            reminder.setTriggerAt(Instant.now().plusSeconds(backoffMinutes * 60));
            reminder.setStatus("PENDING");
            log.info("Reminder: {} rescheduled for retry in {} minutes due to failure: {}", reminder.getId(), backoffMinutes, errorMessage);
        }

        bookingReminderRepository.save(reminder);
    }
}
