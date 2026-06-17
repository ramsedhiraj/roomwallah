package com.roomwallah.booking.application.facade;

import com.roomwallah.booking.domain.entity.LeadNote;
import com.roomwallah.booking.domain.entity.VisitCalendar;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import com.roomwallah.booking.presentation.dto.BookingResponseDto;
import com.roomwallah.booking.presentation.dto.LeadResponseDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.booking.presentation.dto.PropertyVisitResponseDto;
import com.roomwallah.booking.presentation.dto.VisitSlotResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingFacade {
    BookingResponseDto createBooking(UUID tenantId, BookingRequestDto request);
    BookingResponseDto approveBooking(UUID ownerId, UUID bookingId);
    BookingResponseDto rejectBooking(UUID ownerId, UUID bookingId, String reason);
    BookingResponseDto cancelBooking(UUID userId, UUID bookingId);
    List<BookingResponseDto> getTenantBookings(UUID tenantId);
    List<BookingResponseDto> getOwnerBookings(UUID ownerId);
    BookingResponseDto getBooking(UUID id);
    PropertyVisitResponseDto scheduleVisit(UUID tenantId, PropertyVisitRequestDto request);
    PropertyVisitResponseDto recordNoShow(UUID ownerId, UUID visitId);
    PropertyVisitResponseDto completeVisit(UUID ownerId, UUID visitId);
    PropertyVisitResponseDto cancelVisit(UUID userId, UUID visitId);
    List<PropertyVisitResponseDto> getTenantVisits(UUID tenantId);
    List<PropertyVisitResponseDto> getOwnerVisits(UUID ownerId);
    PropertyVisitResponseDto getVisit(UUID id);
    VisitCalendar saveCalendar(UUID ownerId, String recurrenceRulesJson, String blackoutDatesJson, Instant vacationStart, Instant vacationEnd);
    Optional<VisitCalendar> getCalendarByOwner(UUID ownerId);
    List<VisitSlotResponseDto> generateSlots(UUID ownerId, UUID propertyId, Instant start, Instant end);
    List<VisitSlotResponseDto> getSlotsByProperty(UUID propertyId);
    String getIcsCalendar(UUID visitId);
    LeadResponseDto getOrCreateLead(UUID propertyId, UUID tenantId, UUID ownerId, String inquiryText, String phone, String email);
    LeadResponseDto addNote(UUID leadId, UUID authorId, String content);
    LeadResponseDto assignLead(UUID leadId, UUID assigneeId);
    List<LeadResponseDto> getOwnerLeads(UUID ownerId);
    LeadResponseDto getLead(UUID id);
    List<LeadNote> getLeadNotes(UUID leadId);
}
