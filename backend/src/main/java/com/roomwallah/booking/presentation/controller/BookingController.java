package com.roomwallah.booking.presentation.controller;

import com.roomwallah.booking.application.facade.BookingFacade;
import com.roomwallah.booking.domain.event.BookingCancelledEvent;
import com.roomwallah.booking.domain.event.BookingCreatedEvent;
import com.roomwallah.booking.domain.event.VisitCancelledEvent;
import com.roomwallah.booking.domain.event.VisitScheduledEvent;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import com.roomwallah.booking.presentation.dto.BookingResponseDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitResponseDto;
import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingFacade bookingFacade;
    private final CurrentUserProvider currentUserProvider;

    private final List<SseEmitter> tenantEmitters = new CopyOnWriteArrayList<>();

    // 1. Tenant Bookings
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody BookingRequestDto request) {
        
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to create booking by tenant: {}, idempotency key header: {}", currentUser.getId(), idempotencyKeyHeader);
        
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            request.setIdempotencyKey(UUID.randomUUID().toString());
        }

        BookingResponseDto response = bookingFacade.createBooking(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Booking request created successfully"));
    }

    @GetMapping("/bookings/me")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Requesting bookings for tenant: {}", currentUser.getId());
        List<BookingResponseDto> bookings = bookingFacade.getTenantBookings(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDto>> cancelBooking(@PathVariable("id") UUID bookingId) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to cancel booking: {} by user: {}", bookingId, currentUser.getId());
        BookingResponseDto response = bookingFacade.cancelBooking(currentUser.getId(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
    }

    // 2. Tenant Visits
    @PostMapping("/visits")
    public ResponseEntity<ApiResponse<PropertyVisitResponseDto>> scheduleVisit(@Valid @RequestBody PropertyVisitRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to schedule visit for tenant: {} on slot: {}", currentUser.getId(), request.getVisitSlotId());
        PropertyVisitResponseDto response = bookingFacade.scheduleVisit(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Property visit scheduled successfully"));
    }

    @GetMapping("/visits/me")
    public ResponseEntity<ApiResponse<List<PropertyVisitResponseDto>>> getMyVisits() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Requesting visits for tenant: {}", currentUser.getId());
        List<PropertyVisitResponseDto> visits = bookingFacade.getTenantVisits(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(visits));
    }

    @PostMapping("/visits/{id}/cancel")
    public ResponseEntity<ApiResponse<PropertyVisitResponseDto>> cancelVisit(@PathVariable("id") UUID visitId) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to cancel visit: {} by user: {}", visitId, currentUser.getId());
        PropertyVisitResponseDto response = bookingFacade.cancelVisit(currentUser.getId(), visitId);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit cancelled successfully"));
    }

    @GetMapping("/visits/{id}/ics")
    public ResponseEntity<String> downloadIcs(@PathVariable("id") UUID visitId) {
        log.info("Request to export ICS for visit: {}", visitId);
        String icsContent = bookingFacade.getIcsCalendar(visitId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visit_" + visitId + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icsContent);
    }

    // 3. SSE Stream for Tenant updates
    @GetMapping(value = "/bookings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookings() {
        log.info("Registering tenant SSE client");
        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutes timeout
        tenantEmitters.add(emitter);

        emitter.onCompletion(() -> tenantEmitters.remove(emitter));
        emitter.onTimeout(() -> tenantEmitters.remove(emitter));
        emitter.onError(e -> tenantEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Booking Updates Stream"));
        } catch (IOException e) {
            log.error("Failed to send INIT event to tenant SSE", e);
        }

        return emitter;
    }

    // Spring event listeners to notify subscribers
    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        broadcastToTenants("BOOKING_CREATED", event);
    }

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        broadcastToTenants("BOOKING_CANCELLED", event);
    }

    @EventListener
    public void onVisitScheduled(VisitScheduledEvent event) {
        broadcastToTenants("VISIT_SCHEDULED", event);
    }

    @EventListener
    public void onVisitCancelled(VisitCancelledEvent event) {
        broadcastToTenants("VISIT_CANCELLED", event);
    }

    private void broadcastToTenants(String eventName, Object data) {
        log.info("Broadcasting event: {} to all tenant emitters", eventName);
        for (SseEmitter emitter : tenantEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE event to tenant. Removing emitter.", e);
                tenantEmitters.remove(emitter);
            }
        }
    }
}
