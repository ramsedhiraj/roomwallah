package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.VisitCalendar;
import com.roomwallah.booking.domain.entity.VisitSlot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarService {
    VisitCalendar saveCalendar(UUID ownerId, String recurrenceRulesJson, String blackoutDatesJson, Instant vacationStart, Instant vacationEnd);
    Optional<VisitCalendar> getCalendarByOwner(UUID ownerId);
    List<VisitSlot> generateSlots(UUID ownerId, UUID propertyId, Instant start, Instant end);
    String getIcsCalendar(UUID visitId);
    List<VisitSlot> getSlotsByProperty(UUID propertyId);
}
