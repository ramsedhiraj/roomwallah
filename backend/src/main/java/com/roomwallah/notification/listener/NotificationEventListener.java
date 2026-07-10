package com.roomwallah.notification.listener;

import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingStatus;
import com.roomwallah.booking.domain.event.*;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.chat.domain.event.MessageCreatedEvent;
import com.roomwallah.notification.service.NotificationService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.event.PropertyArchivedEvent;
import com.roomwallah.property.domain.event.PropertyPausedEvent;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Handling booking created notification event for booking: {}", event.getBookingId());
        try {
            String message = "New booking request submitted for your property. A chat conversation room has been automatically opened.";
            notificationService.sendNotification(event.getOwnerId(), "New Booking Request Received", message, "BOOKING");
        } catch (Exception e) {
            log.error("Failed to process booking created notification", e);
        }
    }

    @EventListener
    public void onBookingApproved(BookingApprovedEvent event) {
        log.info("Handling booking approved notification event for booking: {}", event.getBookingId());
        try {
            String message = "Your booking request has been approved by the owner!";
            notificationService.sendNotification(event.getTenantId(), "Booking Request Approved", message, "BOOKING");
        } catch (Exception e) {
            log.error("Failed to process booking approved notification", e);
        }
    }

    @EventListener
    public void onBookingRejected(BookingRejectedEvent event) {
        log.info("Handling booking rejected notification event for booking: {}", event.getBookingId());
        try {
            String message = "Your booking request was rejected. Reason: " + event.getReason();
            notificationService.sendNotification(event.getTenantId(), "Booking Request Rejected", message, "BOOKING");
        } catch (Exception e) {
            log.error("Failed to process booking rejected notification", e);
        }
    }

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Handling booking cancelled notification event for booking: {}", event.getBookingId());
        try {
            if ("TENANT".equalsIgnoreCase(event.getCancelledBy())) {
                String message = "The tenant has cancelled their booking request.";
                notificationService.sendNotification(event.getOwnerId(), "Booking Request Cancelled", message, "BOOKING");
            } else {
                String message = "The owner has cancelled your booking request.";
                notificationService.sendNotification(event.getTenantId(), "Booking Request Cancelled", message, "BOOKING");
            }
        } catch (Exception e) {
            log.error("Failed to process booking cancelled notification", e);
        }
    }

    @EventListener
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.info("Handling booking completed notification event for booking: {}", event.getBookingId());
        try {
            String message = "Your booking has been marked as completed.";
            notificationService.sendNotification(event.getTenantId(), "Booking Completed", message, "BOOKING");
        } catch (Exception e) {
            log.error("Failed to process booking completed notification", e);
        }
    }

    @EventListener
    public void onMessageCreated(MessageCreatedEvent event) {
        log.info("Handling message created notification event for message: {}", event.getMessageId());
        try {
            User sender = userRepository.findById(event.getSenderId()).orElse(null);
            String senderName = sender != null ? sender.getFullName() : "A user";
            String preview = event.getContent().length() > 50 ? event.getContent().substring(0, 47) + "..." : event.getContent();
            String message = senderName + " sent a message: " + preview;
            notificationService.sendNotification(event.getRecipientId(), "New Message Received", message, "CHAT");
        } catch (Exception e) {
            log.error("Failed to process message created notification", e);
        }
    }

    @EventListener
    public void onPropertyPaused(PropertyPausedEvent event) {
        log.info("Handling property paused notification event for property: {}", event.getPropertyId());
        try {
            Property property = propertyRepository.findById(event.getPropertyId()).orElse(null);
            String title = property != null ? property.getTitle() : "Property listing";
            List<Booking> bookings = bookingRepository.findByPropertyId(event.getPropertyId());
            for (Booking booking : bookings) {
                if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                    String message = "The listing \"" + title + "\" is temporarily unavailable.";
                    notificationService.sendNotification(booking.getTenantId(), "Listing Unavailable", message, "BOOKING");
                }
            }
        } catch (Exception e) {
            log.error("Failed to process property paused notification", e);
        }
    }

    @EventListener
    public void onPropertyArchived(PropertyArchivedEvent event) {
        log.info("Handling property archived notification event for property: {}", event.getPropertyId());
        try {
            Property property = propertyRepository.findById(event.getPropertyId()).orElse(null);
            String title = property != null ? property.getTitle() : "Property listing";
            List<Booking> bookings = bookingRepository.findByPropertyId(event.getPropertyId());
            for (Booking booking : bookings) {
                if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                    String message = "The listing \"" + title + "\" is no longer available.";
                    notificationService.sendNotification(booking.getTenantId(), "Listing Unavailable", message, "BOOKING");
                }
            }
        } catch (Exception e) {
            log.error("Failed to process property archived notification", e);
        }
    }
}
