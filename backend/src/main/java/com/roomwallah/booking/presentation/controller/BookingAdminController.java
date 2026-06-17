package com.roomwallah.booking.presentation.controller;

import com.roomwallah.booking.application.facade.BookingFacade;
import com.roomwallah.booking.domain.entity.LeadNote;
import com.roomwallah.booking.domain.entity.VisitCalendar;
import com.roomwallah.booking.domain.event.BookingApprovedEvent;
import com.roomwallah.booking.domain.event.BookingRejectedEvent;
import com.roomwallah.booking.domain.event.VisitCompletedEvent;
import com.roomwallah.booking.domain.event.VisitNoShowEvent;
import com.roomwallah.booking.presentation.dto.BookingResponseDto;
import com.roomwallah.booking.presentation.dto.LeadAssignmentRequestDto;
import com.roomwallah.booking.presentation.dto.LeadNoteRequestDto;
import com.roomwallah.booking.presentation.dto.LeadResponseDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitResponseDto;
import com.roomwallah.booking.presentation.dto.VisitSlotResponseDto;
import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class BookingAdminController {

    private final BookingFacade bookingFacade;
    private final CurrentUserProvider currentUserProvider;

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    // 1. Booking Approvals & Rejections
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getOwnerBookings() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Requesting bookings for owner/admin: {}", currentUser.getId());
        List<BookingResponseDto> bookings = bookingFacade.getOwnerBookings(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PostMapping("/bookings/{id}/approve")
    public ResponseEntity<ApiResponse<BookingResponseDto>> approveBooking(@PathVariable("id") UUID bookingId) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to approve booking: {} by owner/admin: {}", bookingId, currentUser.getId());
        BookingResponseDto response = bookingFacade.approveBooking(currentUser.getId(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking approved successfully"));
    }

    @PostMapping("/bookings/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponseDto>> rejectBooking(
            @PathVariable("id") UUID bookingId,
            @RequestParam("reason") String reason) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to reject booking: {} by owner/admin: {} for reason: {}", bookingId, currentUser.getId(), reason);
        BookingResponseDto response = bookingFacade.rejectBooking(currentUser.getId(), bookingId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking rejected successfully"));
    }

    // 2. CRM Leads Management
    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<List<LeadResponseDto>>> getLeads() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Requesting leads list for owner/admin: {}", currentUser.getId());
        List<LeadResponseDto> leads = bookingFacade.getOwnerLeads(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @PostMapping("/leads/{id}/notes")
    public ResponseEntity<ApiResponse<LeadResponseDto>> addLeadNote(
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody LeadNoteRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Adding CRM lead note for lead: {} by user: {}", leadId, currentUser.getId());
        LeadResponseDto response = bookingFacade.addNote(leadId, currentUser.getId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "CRM note logged successfully"));
    }

    @GetMapping("/leads/{id}/notes")
    public ResponseEntity<ApiResponse<List<LeadNote>>> getLeadNotes(@PathVariable("id") UUID leadId) {
        log.info("Requesting CRM notes audit for lead: {}", leadId);
        List<LeadNote> notes = bookingFacade.getLeadNotes(leadId);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @PostMapping("/leads/{id}/assign")
    public ResponseEntity<ApiResponse<LeadResponseDto>> assignLead(
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody LeadAssignmentRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Manually assigning lead: {} to agent: {} by user: {}", leadId, request.getAssigneeId(), currentUser.getId());
        LeadResponseDto response = bookingFacade.assignLead(leadId, request.getAssigneeId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lead assigned successfully"));
    }

    // 3. Visit slots generation & Calendar rules management
    @PostMapping("/calendar")
    public ResponseEntity<ApiResponse<VisitCalendar>> saveCalendarRules(
            @RequestParam("rules") String recurrenceRulesJson,
            @RequestParam(value = "blackouts", required = false) String blackoutDatesJson,
            @RequestParam(value = "vacationStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant vacationStart,
            @RequestParam(value = "vacationEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant vacationEnd) {
        
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Saving calendar availability rules for owner: {}", currentUser.getId());
        VisitCalendar calendar = bookingFacade.saveCalendar(currentUser.getId(), recurrenceRulesJson, blackoutDatesJson, vacationStart, vacationEnd);
        return ResponseEntity.ok(ApiResponse.success(calendar, "Calendar configuration updated successfully"));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<VisitCalendar>> getCalendarRules() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Fetching calendar availability rules for owner: {}", currentUser.getId());
        Optional<VisitCalendar> calendar = bookingFacade.getCalendarByOwner(currentUser.getId());
        return calendar.map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Calendar config not found")));
    }

    @PostMapping("/calendar/slots")
    public ResponseEntity<ApiResponse<List<VisitSlotResponseDto>>> generateSlots(
            @RequestParam("propertyId") UUID propertyId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Generating batch slots for property: {} from {} to {} by owner: {}", propertyId, start, end, currentUser.getId());
        List<VisitSlotResponseDto> slots = bookingFacade.generateSlots(currentUser.getId(), propertyId, start, end);
        return ResponseEntity.ok(ApiResponse.success(slots, "Visit slots generated successfully"));
    }

    @GetMapping("/calendar/slots")
    public ResponseEntity<ApiResponse<List<VisitSlotResponseDto>>> getPropertySlots(@RequestParam("propertyId") UUID propertyId) {
        log.info("Fetching slots for property: {}", propertyId);
        List<VisitSlotResponseDto> slots = bookingFacade.getSlotsByProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    // 4. Record visit outcomes (no-show, completed)
    @PostMapping("/visits/{id}/noshow")
    public ResponseEntity<ApiResponse<PropertyVisitResponseDto>> recordNoShow(@PathVariable("id") UUID visitId) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Recording visit no-show for visit ID: {} by owner: {}", visitId, currentUser.getId());
        PropertyVisitResponseDto response = bookingFacade.recordNoShow(currentUser.getId(), visitId);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit logged as no-show"));
    }

    @PostMapping("/visits/{id}/complete")
    public ResponseEntity<ApiResponse<PropertyVisitResponseDto>> completeVisit(@PathVariable("id") UUID visitId) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Completing visit ID: {} by owner: {}", visitId, currentUser.getId());
        PropertyVisitResponseDto response = bookingFacade.completeVisit(currentUser.getId(), visitId);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit logged as completed"));
    }

    // 5. SSE Updates stream for owners/admins
    @GetMapping(value = "/bookings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookings() {
        log.info("Registering owner/admin SSE client");
        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutes timeout
        adminEmitters.add(emitter);

        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError(e -> adminEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Admin Booking Updates Stream"));
        } catch (IOException e) {
            log.error("Failed to send INIT event to admin SSE", e);
        }

        return emitter;
    }

    @EventListener
    public void onBookingApproved(BookingApprovedEvent event) {
        broadcastToAdmins("BOOKING_APPROVED", event);
    }

    @EventListener
    public void onBookingRejected(BookingRejectedEvent event) {
        broadcastToAdmins("BOOKING_REJECTED", event);
    }

    @EventListener
    public void onVisitCompleted(VisitCompletedEvent event) {
        broadcastToAdmins("VISIT_COMPLETED", event);
    }

    @EventListener
    public void onVisitNoShow(VisitNoShowEvent event) {
        broadcastToAdmins("VISIT_NOSHOW", event);
    }

    private void broadcastToAdmins(String eventName, Object data) {
        log.info("Broadcasting event: {} to all admin emitters", eventName);
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE event to admin. Removing emitter.", e);
                adminEmitters.remove(emitter);
            }
        }
    }
}
