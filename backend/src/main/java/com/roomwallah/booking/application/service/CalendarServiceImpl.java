package com.roomwallah.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.SlotStatus;
import com.roomwallah.booking.domain.entity.VisitCalendar;
import com.roomwallah.booking.domain.entity.VisitSlot;
import com.roomwallah.booking.domain.port.CalendarSyncPort;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.booking.domain.repository.VisitCalendarRepository;
import com.roomwallah.booking.domain.repository.VisitSlotRepository;
import com.roomwallah.booking.domain.valueobject.CalendarAvailabilityRule;
import com.roomwallah.booking.infrastructure.adapter.IcsCalendarGenerator;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final VisitCalendarRepository visitCalendarRepository;
    private final VisitSlotRepository visitSlotRepository;
    private final PropertyVisitRepository propertyVisitRepository;
    private final PropertyRepository propertyRepository;
    private final List<CalendarSyncPort> calendarSyncPorts;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public VisitCalendar saveCalendar(UUID ownerId, String recurrenceRulesJson, String blackoutDatesJson, Instant vacationStart, Instant vacationEnd) {
        log.info("Saving calendar for owner: {}", ownerId);
        VisitCalendar calendar = visitCalendarRepository.findByOwnerId(ownerId)
                .orElse(new VisitCalendar());

        calendar.setOwnerId(ownerId);
        calendar.setRecurrenceRulesJson(recurrenceRulesJson);
        calendar.setBlackoutDatesJson(blackoutDatesJson);
        calendar.setVacationStart(vacationStart);
        calendar.setVacationEnd(vacationEnd);

        return visitCalendarRepository.save(calendar);
    }

    @Override
    public Optional<VisitCalendar> getCalendarByOwner(UUID ownerId) {
        return visitCalendarRepository.findByOwnerId(ownerId);
    }

    @Override
    @Transactional
    public List<VisitSlot> generateSlots(UUID ownerId, UUID propertyId, Instant start, Instant end) {
        log.info("Generating availability slots for property: {} from {} to {}", propertyId, start, end);
        VisitCalendar calendar = visitCalendarRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner calendar not configured"));

        List<CalendarAvailabilityRule> rules = new ArrayList<>();
        if (calendar.getRecurrenceRulesJson() != null && !calendar.getRecurrenceRulesJson().isBlank()) {
            try {
                rules = objectMapper.readValue(calendar.getRecurrenceRulesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, CalendarAvailabilityRule.class));
            } catch (Exception e) {
                log.error("Failed to parse calendar recurrence rules", e);
            }
        }

        List<String> blackoutDates = new ArrayList<>();
        if (calendar.getBlackoutDatesJson() != null && !calendar.getBlackoutDatesJson().isBlank()) {
            try {
                blackoutDates = objectMapper.readValue(calendar.getBlackoutDatesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                log.error("Failed to parse blackout dates", e);
            }
        }

        List<VisitSlot> existingSlots = visitSlotRepository.findByPropertyId(propertyId);
        List<VisitSlot> generated = new ArrayList<>();

        ZonedDateTime current = start.atZone(ZoneOffset.UTC);
        ZonedDateTime limit = end.atZone(ZoneOffset.UTC);

        while (current.isBefore(limit)) {
            // Check vacation range
            Instant currentInstant = current.toInstant();
            if (calendar.getVacationStart() != null && calendar.getVacationEnd() != null) {
                if (!currentInstant.isBefore(calendar.getVacationStart()) && !currentInstant.isAfter(calendar.getVacationEnd())) {
                    current = current.plusDays(1);
                    continue;
                }
            }

            // Check blackout dates
            String dateString = current.toLocalDate().toString();
            if (blackoutDates.contains(dateString)) {
                current = current.plusDays(1);
                continue;
            }

            // Match day of week rules
            String dayOfWeekName = current.getDayOfWeek().name();
            Optional<CalendarAvailabilityRule> matchedRule = rules.stream()
                    .filter(r -> r.getDayOfWeek().equalsIgnoreCase(dayOfWeekName) && r.isAvailable())
                    .findFirst();

            if (matchedRule.isPresent()) {
                CalendarAvailabilityRule rule = matchedRule.get();
                try {
                    String[] startParts = rule.getStartTime().split(":");
                    String[] endParts = rule.getEndTime().split(":");

                    int startHour = Integer.parseInt(startParts[0]);
                    int startMin = Integer.parseInt(startParts[1]);
                    int endHour = Integer.parseInt(endParts[0]);
                    int endMin = Integer.parseInt(endParts[1]);

                    ZonedDateTime slotStart = current.withHour(startHour).withMinute(startMin).withSecond(0).withNano(0);
                    ZonedDateTime slotLimit = current.withHour(endHour).withMinute(endMin).withSecond(0).withNano(0);

                    while (slotStart.isBefore(slotLimit)) {
                        ZonedDateTime slotEnd = slotStart.plusHours(1);
                        if (!slotEnd.isAfter(slotLimit)) {
                            Instant slotStartInstant = slotStart.toInstant();
                            boolean exists = existingSlots.stream().anyMatch(es -> es.getStartTime().equals(slotStartInstant));
                            if (!exists) {
                                VisitSlot slot = new VisitSlot();
                                slot.setPropertyId(propertyId);
                                slot.setStartTime(slotStartInstant);
                                slot.setEndTime(slotEnd.toInstant());
                                slot.setMaxBookings(2); // default max bookings
                                slot.setCurrentBookings(0);
                                slot.setStatus(SlotStatus.AVAILABLE);

                                VisitSlot savedSlot = visitSlotRepository.save(slot);
                                generated.add(savedSlot);
                            }
                        }
                        slotStart = slotEnd;
                    }
                } catch (Exception e) {
                    log.error("Failed to generate slots for rule on day: {}", dayOfWeekName, e);
                }
            }
            current = current.plusDays(1);
        }

        // Provider-agnostic calendar synchronization sync trigger
        if (!generated.isEmpty()) {
            for (CalendarSyncPort syncPort : calendarSyncPorts) {
                try {
                    syncPort.exportEvent(propertyId, "Property Slots Generated", start, end);
                } catch (Exception e) {
                    log.warn("Calendar Sync Port failed to synchronize for provider: {}", syncPort.getClass().getSimpleName(), e);
                }
            }
        }

        return generated;
    }

    @Override
    public String getIcsCalendar(UUID visitId) {
        log.info("Generating ICS calendar for visit ID: {}", visitId);
        PropertyVisit visit = propertyVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Property visit not found"));

        Property property = propertyRepository.findById(visit.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        String summary = "RoomWallah Visit - " + property.getTitle();
        String description = "Scheduled visit for property: " + property.getListingRef() + ". Notes: " + (visit.getNotes() != null ? visit.getNotes() : "None");

        return IcsCalendarGenerator.generateIcs(visit.getId(), summary, description, visit.getStartTime(), visit.getEndTime());
    }

    @Override
    public List<VisitSlot> getSlotsByProperty(UUID propertyId) {
        return visitSlotRepository.findByPropertyId(propertyId);
    }
}
