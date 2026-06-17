package com.roomwallah.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.BookingHistory;
import com.roomwallah.booking.domain.entity.BookingOutbox;
import com.roomwallah.booking.domain.entity.BookingStatus;
import com.roomwallah.booking.domain.event.BookingApprovedEvent;
import com.roomwallah.booking.domain.event.BookingCancelledEvent;
import com.roomwallah.booking.domain.event.BookingCreatedEvent;
import com.roomwallah.booking.domain.event.BookingRejectedEvent;
import com.roomwallah.booking.domain.repository.BookingHistoryRepository;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final BookingOutboxRepository bookingOutboxRepository;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    @Value("${roomwallah.booking.instant-booking.enabled:false}")
    private boolean defaultInstantBookingEnabled;

    @Override
    @Transactional
    public Booking createBooking(UUID tenantId, BookingRequestDto request) {
        log.info("Creating booking for tenant: {}, property: {}", tenantId, request.getPropertyId());

        // 1. Idempotency Check
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate request detected for idempotency key: {}. Returning existing booking: {}",
                        request.getIdempotencyKey(), existing.get().getId());
                return existing.get();
            }
        }

        // 2. Validate Property
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId()));

        if (property.getStatus() != PropertyStatus.ACTIVE) {
            log.warn("Property: {} is not ACTIVE, status is: {}", property.getId(), property.getStatus());
            throw new IllegalStateException("Property is not available for booking");
        }

        // 3. Conflict Detection (Ensure no other pending/confirmed booking by same tenant for same property)
        List<Booking> activeBookings = bookingRepository.findByTenantId(tenantId);
        boolean hasConflict = activeBookings.stream()
                .anyMatch(b -> b.getPropertyId().equals(request.getPropertyId()) &&
                        (b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED));

        if (hasConflict) {
            log.warn("Conflict detected: tenant {} already has an active booking for property {}", tenantId, property.getId());
            throw new IllegalStateException("Tenant already has an active or pending booking for this property");
        }

        // 4. Create Booking
        Booking booking = new Booking();
        booking.setPropertyId(property.getId());
        booking.setTenantId(tenantId);
        booking.setOwnerId(property.getOwnerId());
        booking.setPriceAmount(request.getPriceAmount());
        booking.setPriceCurrency(request.getPriceCurrency());
        booking.setNotes(request.getNotes());
        booking.setIdempotencyKey(request.getIdempotencyKey());

        // Determine if instant booking is enabled (can be enabled via global properties or property level config mock)
        boolean instantBooking = defaultInstantBookingEnabled;
        booking.setStatus(instantBooking ? BookingStatus.CONFIRMED : BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // Record history
        recordHistory(savedBooking.getId(), null, savedBooking.getStatus(), "Booking initialized");

        // Publish event to Outbox
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(savedBooking.getId())
                .propertyId(savedBooking.getPropertyId())
                .tenantId(savedBooking.getTenantId())
                .ownerId(savedBooking.getOwnerId())
                .priceAmount(savedBooking.getPriceAmount())
                .priceCurrency(savedBooking.getPriceCurrency())
                .notes(savedBooking.getNotes())
                .createdAt(savedBooking.getCreatedAt() != null ? savedBooking.getCreatedAt() : Instant.now())
                .build();
        saveToOutbox("BookingCreatedEvent", savedBooking.getId(), event);

        log.info("Booking created successfully with ID: {}, status: {}", savedBooking.getId(), savedBooking.getStatus());
        return savedBooking;
    }

    @Override
    @Transactional
    public Booking approveBooking(UUID ownerId, UUID bookingId) {
        log.info("Approving booking ID: {} by owner: {}", bookingId, ownerId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Unauthorized owner operation");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking must be in PENDING status to be approved");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        recordHistory(savedBooking.getId(), oldStatus, savedBooking.getStatus(), "Booking approved by owner");

        BookingApprovedEvent event = BookingApprovedEvent.builder()
                .bookingId(savedBooking.getId())
                .propertyId(savedBooking.getPropertyId())
                .tenantId(savedBooking.getTenantId())
                .ownerId(savedBooking.getOwnerId())
                .approvedAt(Instant.now())
                .build();
        saveToOutbox("BookingApprovedEvent", savedBooking.getId(), event);

        return savedBooking;
    }

    @Override
    @Transactional
    public Booking rejectBooking(UUID ownerId, UUID bookingId, String reason) {
        log.info("Rejecting booking ID: {} by owner: {}, reason: {}", bookingId, ownerId, reason);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Unauthorized owner operation");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking must be in PENDING status to be rejected");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.REJECTED);
        Booking savedBooking = bookingRepository.save(booking);

        recordHistory(savedBooking.getId(), oldStatus, savedBooking.getStatus(), "Booking rejected: " + reason);

        BookingRejectedEvent event = BookingRejectedEvent.builder()
                .bookingId(savedBooking.getId())
                .propertyId(savedBooking.getPropertyId())
                .tenantId(savedBooking.getTenantId())
                .ownerId(savedBooking.getOwnerId())
                .reason(reason)
                .rejectedAt(Instant.now())
                .build();
        saveToOutbox("BookingRejectedEvent", savedBooking.getId(), event);

        return savedBooking;
    }

    @Override
    @Transactional
    public Booking cancelBooking(UUID userId, UUID bookingId) {
        log.info("Cancelling booking ID: {} by user: {}", bookingId, userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getTenantId().equals(userId) && !booking.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized cancel operation");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled from status: " + booking.getStatus());
        }

        String cancelledBy = booking.getTenantId().equals(userId) ? "TENANT" : "OWNER";
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        recordHistory(savedBooking.getId(), oldStatus, savedBooking.getStatus(), "Booking cancelled by " + cancelledBy);

        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(savedBooking.getId())
                .propertyId(savedBooking.getPropertyId())
                .tenantId(savedBooking.getTenantId())
                .ownerId(savedBooking.getOwnerId())
                .cancelledBy(cancelledBy)
                .cancelledAt(Instant.now())
                .build();
        saveToOutbox("BookingCancelledEvent", savedBooking.getId(), event);

        return savedBooking;
    }

    @Override
    public List<Booking> getTenantBookings(UUID tenantId) {
        return bookingRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Booking> getOwnerBookings(UUID ownerId) {
        return bookingRepository.findByOwnerId(ownerId);
    }

    @Override
    public Optional<Booking> getBooking(UUID id) {
        return bookingRepository.findById(id);
    }

    @Override
    @Transactional
    public void expireUnconfirmedBookings() {
        log.info("Running job to expire unconfirmed bookings");
        Instant expiryThreshold = Instant.now().minusSeconds(86400); // 24 hours ago
        // In-memory filter or db search. Let's do simple db load & check to keep it simple and clean
        List<Booking> bookings = bookingRepository.findAll();
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.PENDING &&
                    booking.getCreatedAt() != null && booking.getCreatedAt().isBefore(expiryThreshold)) {
                log.info("Expiring booking ID: {} created at: {}", booking.getId(), booking.getCreatedAt());
                booking.setStatus(BookingStatus.REJECTED);
                bookingRepository.save(booking);
                recordHistory(booking.getId(), BookingStatus.PENDING, BookingStatus.REJECTED, "Auto-expired (unconfirmed for 24h)");
            }
        }
    }

    private void recordHistory(UUID bookingId, BookingStatus statusFrom, BookingStatus statusTo, String notes) {
        BookingHistory history = new BookingHistory();
        history.setBookingId(bookingId);
        history.setStatusFrom(statusFrom != null ? statusFrom.name() : null);
        history.setStatusTo(statusTo.name());
        history.setNotes(notes);
        bookingHistoryRepository.save(history);
    }

    private void saveToOutbox(String eventType, UUID eventId, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            BookingOutbox outbox = new BookingOutbox();
            outbox.setEventType(eventType);
            outbox.setEventId(eventId);
            outbox.setPayloadJson(payload);
            outbox.setStatus("PENDING");
            bookingOutboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event", e);
            throw new RuntimeException("Outbox serialization error", e);
        }
    }
}
