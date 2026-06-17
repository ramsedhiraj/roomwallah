package com.roomwallah.booking.application.facade;

import com.roomwallah.booking.application.service.BookingService;
import com.roomwallah.booking.application.service.CalendarService;
import com.roomwallah.booking.application.service.LeadService;
import com.roomwallah.booking.application.service.VisitService;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.entity.Lead;
import com.roomwallah.booking.domain.entity.LeadNote;
import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.VisitCalendar;
import com.roomwallah.booking.domain.entity.VisitSlot;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import com.roomwallah.booking.presentation.dto.BookingResponseDto;
import com.roomwallah.booking.presentation.dto.LeadResponseDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitResponseDto;
import com.roomwallah.booking.presentation.dto.VisitSlotResponseDto;
import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingFacadeImpl implements BookingFacade {

    private final BookingService bookingService;
    private final VisitService visitService;
    private final CalendarService calendarService;
    private final LeadService leadService;

    @Override
    public BookingResponseDto createBooking(UUID tenantId, BookingRequestDto request) {
        Booking booking = bookingService.createBooking(tenantId, request);
        return mapToBookingResponse(booking);
    }

    @Override
    public BookingResponseDto approveBooking(UUID ownerId, UUID bookingId) {
        Booking booking = bookingService.approveBooking(ownerId, bookingId);
        return mapToBookingResponse(booking);
    }

    @Override
    public BookingResponseDto rejectBooking(UUID ownerId, UUID bookingId, String reason) {
        Booking booking = bookingService.rejectBooking(ownerId, bookingId, reason);
        return mapToBookingResponse(booking);
    }

    @Override
    public BookingResponseDto cancelBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingService.cancelBooking(userId, bookingId);
        return mapToBookingResponse(booking);
    }

    @Override
    public List<BookingResponseDto> getTenantBookings(UUID tenantId) {
        return bookingService.getTenantBookings(tenantId).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDto> getOwnerBookings(UUID ownerId) {
        return bookingService.getOwnerBookings(ownerId).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponseDto getBooking(UUID id) {
        Booking booking = bookingService.getBooking(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        return mapToBookingResponse(booking);
    }

    @Override
    public PropertyVisitResponseDto scheduleVisit(UUID tenantId, PropertyVisitRequestDto request) {
        PropertyVisit visit = visitService.scheduleVisit(tenantId, request);
        return mapToVisitResponse(visit);
    }

    @Override
    public PropertyVisitResponseDto recordNoShow(UUID ownerId, UUID visitId) {
        PropertyVisit visit = visitService.recordNoShow(ownerId, visitId);
        return mapToVisitResponse(visit);
    }

    @Override
    public PropertyVisitResponseDto completeVisit(UUID ownerId, UUID visitId) {
        PropertyVisit visit = visitService.completeVisit(ownerId, visitId);
        return mapToVisitResponse(visit);
    }

    @Override
    public PropertyVisitResponseDto cancelVisit(UUID userId, UUID visitId) {
        PropertyVisit visit = visitService.cancelVisit(userId, visitId);
        return mapToVisitResponse(visit);
    }

    @Override
    public List<PropertyVisitResponseDto> getTenantVisits(UUID tenantId) {
        return visitService.getTenantVisits(tenantId).stream()
                .map(this::mapToVisitResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PropertyVisitResponseDto> getOwnerVisits(UUID ownerId) {
        return visitService.getOwnerVisits(ownerId).stream()
                .map(this::mapToVisitResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PropertyVisitResponseDto getVisit(UUID id) {
        PropertyVisit visit = visitService.getVisit(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + id));
        return mapToVisitResponse(visit);
    }

    @Override
    public VisitCalendar saveCalendar(UUID ownerId, String recurrenceRulesJson, String blackoutDatesJson, Instant vacationStart, Instant vacationEnd) {
        return calendarService.saveCalendar(ownerId, recurrenceRulesJson, blackoutDatesJson, vacationStart, vacationEnd);
    }

    @Override
    public Optional<VisitCalendar> getCalendarByOwner(UUID ownerId) {
        return calendarService.getCalendarByOwner(ownerId);
    }

    @Override
    public List<VisitSlotResponseDto> generateSlots(UUID ownerId, UUID propertyId, Instant start, Instant end) {
        return calendarService.generateSlots(ownerId, propertyId, start, end).stream()
                .map(this::mapToSlotResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitSlotResponseDto> getSlotsByProperty(UUID propertyId) {
        return calendarService.getSlotsByProperty(propertyId).stream()
                .map(this::mapToSlotResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String getIcsCalendar(UUID visitId) {
        return calendarService.getIcsCalendar(visitId);
    }

    @Override
    public LeadResponseDto getOrCreateLead(UUID propertyId, UUID tenantId, UUID ownerId, String inquiryText, String phone, String email) {
        Lead lead = leadService.getOrCreateLead(propertyId, tenantId, ownerId, inquiryText, phone, email);
        return mapToLeadResponse(lead);
    }

    @Override
    public LeadResponseDto addNote(UUID leadId, UUID authorId, String content) {
        leadService.addNote(leadId, authorId, content);
        Lead lead = leadService.getLead(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        return mapToLeadResponse(lead);
    }

    @Override
    public LeadResponseDto assignLead(UUID leadId, UUID assigneeId) {
        leadService.assignLead(leadId, assigneeId);
        Lead lead = leadService.getLead(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        return mapToLeadResponse(lead);
    }

    @Override
    public List<LeadResponseDto> getOwnerLeads(UUID ownerId) {
        return leadService.getOwnerLeads(ownerId).stream()
                .map(this::mapToLeadResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LeadResponseDto getLead(UUID id) {
        Lead lead = leadService.getLead(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        return mapToLeadResponse(lead);
    }

    @Override
    public List<LeadNote> getLeadNotes(UUID leadId) {
        return leadService.getLeadNotes(leadId);
    }

    private BookingResponseDto mapToBookingResponse(Booking booking) {
        return BookingResponseDto.builder()
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .tenantId(booking.getTenantId())
                .ownerId(booking.getOwnerId())
                .status(booking.getStatus())
                .priceAmount(booking.getPriceAmount())
                .priceCurrency(booking.getPriceCurrency())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private PropertyVisitResponseDto mapToVisitResponse(PropertyVisit visit) {
        return PropertyVisitResponseDto.builder()
                .id(visit.getId())
                .propertyId(visit.getPropertyId())
                .tenantId(visit.getTenantId())
                .visitSlotId(visit.getVisitSlotId())
                .status(visit.getStatus())
                .startTime(visit.getStartTime())
                .endTime(visit.getEndTime())
                .notes(visit.getNotes())
                .createdAt(visit.getCreatedAt())
                .build();
    }

    private VisitSlotResponseDto mapToSlotResponse(VisitSlot slot) {
        return VisitSlotResponseDto.builder()
                .id(slot.getId())
                .propertyId(slot.getPropertyId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .maxBookings(slot.getMaxBookings())
                .currentBookings(slot.getCurrentBookings())
                .status(slot.getStatus())
                .build();
    }

    private LeadResponseDto mapToLeadResponse(Lead lead) {
        return LeadResponseDto.builder()
                .id(lead.getId())
                .propertyId(lead.getPropertyId())
                .tenantId(lead.getTenantId())
                .ownerId(lead.getOwnerId())
                .status(lead.getStatus())
                .inquiryText(lead.getInquiryText())
                .contactPhone(lead.getContactPhone())
                .contactEmail(lead.getContactEmail())
                .leadScore(lead.getLeadScore())
                .leadScoreExplanation(lead.getLeadScoreExplanation())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}
