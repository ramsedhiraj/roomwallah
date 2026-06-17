package com.roomwallah.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.booking.domain.entity.BookingOutbox;
import com.roomwallah.booking.domain.entity.BookingReminder;
import com.roomwallah.booking.domain.entity.Lead;
import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.ReminderType;
import com.roomwallah.booking.domain.entity.SlotStatus;
import com.roomwallah.booking.domain.entity.VisitSlot;
import com.roomwallah.booking.domain.entity.VisitStatus;
import com.roomwallah.booking.domain.entity.WaitlistEntry;
import com.roomwallah.booking.domain.event.VisitCancelledEvent;
import com.roomwallah.booking.domain.event.VisitCompletedEvent;
import com.roomwallah.booking.domain.event.VisitNoShowEvent;
import com.roomwallah.booking.domain.event.VisitScheduledEvent;
import com.roomwallah.booking.domain.port.LeadScoringPort;
import com.roomwallah.booking.domain.repository.BookingOutboxRepository;
import com.roomwallah.booking.domain.repository.BookingReminderRepository;
import com.roomwallah.booking.domain.repository.LeadRepository;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.booking.domain.repository.VisitSlotRepository;
import com.roomwallah.booking.domain.repository.WaitlistEntryRepository;
import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitSlotRepository visitSlotRepository;
    private final PropertyVisitRepository propertyVisitRepository;
    private final BookingReminderRepository bookingReminderRepository;
    private final BookingOutboxRepository bookingOutboxRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final PropertyRepository propertyRepository;
    private final LeadRepository leadRepository;
    private final LeadScoringPort leadScoringPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PropertyVisit scheduleVisit(UUID tenantId, PropertyVisitRequestDto request) {
        log.info("Scheduling visit for tenant: {} on slot: {}", tenantId, request.getVisitSlotId());

        // Lock visit slot to prevent concurrency/overselling issues
        VisitSlot slot = visitSlotRepository.findAndLockById(request.getVisitSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit slot not found with ID: " + request.getVisitSlotId()));

        if (slot.getStatus() == SlotStatus.CANCELLED || slot.getStatus() == SlotStatus.EXPIRED) {
            throw new IllegalStateException("Visit slot is not available for booking");
        }

        // Check if slot has space
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            log.info("Slot {} is full. Adding tenant {} to waitlist.", slot.getId(), tenantId);
            // Create waitlist entry
            List<WaitlistEntry> waitlist = waitlistEntryRepository.findByVisitSlotIdOrderByPriorityAsc(slot.getId());
            int nextPriority = waitlist.isEmpty() ? 1 : waitlist.get(waitlist.size() - 1).getPriority() + 1;

            WaitlistEntry entry = new WaitlistEntry();
            entry.setVisitSlotId(slot.getId());
            entry.setTenantId(tenantId);
            entry.setPriority(nextPriority);
            entry.setStatus("PENDING");
            waitlistEntryRepository.save(entry);

            throw new IllegalStateException("Visit slot is full. You have been added to the waitlist.");
        }

        // Increment current bookings
        slot.setCurrentBookings(slot.getCurrentBookings() + 1);
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            slot.setStatus(SlotStatus.BOOKED);
        }
        visitSlotRepository.save(slot);

        // Retrieve property details
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        // Create visit
        PropertyVisit visit = new PropertyVisit();
        visit.setPropertyId(property.getId());
        visit.setTenantId(tenantId);
        visit.setVisitSlotId(slot.getId());
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setStartTime(slot.getStartTime());
        visit.setEndTime(slot.getEndTime());
        visit.setNotes(request.getNotes());
        PropertyVisit savedVisit = propertyVisitRepository.save(visit);

        // Schedule Reminder (24 hours prior)
        createReminder(savedVisit);

        // Qualify lead CRM or update lead details
        qualifyLead(savedVisit.getPropertyId(), savedVisit.getTenantId(), property.getOwnerId());

        // Publish event to Outbox
        VisitScheduledEvent event = VisitScheduledEvent.builder()
                .visitId(savedVisit.getId())
                .propertyId(savedVisit.getPropertyId())
                .tenantId(savedVisit.getTenantId())
                .visitSlotId(savedVisit.getVisitSlotId())
                .startTime(savedVisit.getStartTime())
                .endTime(savedVisit.getEndTime())
                .scheduledAt(Instant.now())
                .build();
        saveToOutbox("VisitScheduledEvent", savedVisit.getId(), event);

        return savedVisit;
    }

    @Override
    @Transactional
    public PropertyVisit recordNoShow(UUID ownerId, UUID visitId) {
        log.info("Recording no-show for visit ID: {} by owner: {}", visitId, ownerId);
        PropertyVisit visit = propertyVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        // Validate property owner ownership
        Property property = propertyRepository.findById(visit.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (!property.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Unauthorized owner operation");
        }

        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            throw new IllegalStateException("Visit must be in SCHEDULED status to record no-show");
        }

        visit.setStatus(VisitStatus.NO_SHOW);
        PropertyVisit savedVisit = propertyVisitRepository.save(visit);

        // Update Lead Score
        recalculateLeadScore(visit.getPropertyId(), visit.getTenantId(), property.getOwnerId());

        // Publish event to Outbox
        VisitNoShowEvent event = VisitNoShowEvent.builder()
                .visitId(savedVisit.getId())
                .propertyId(savedVisit.getPropertyId())
                .tenantId(savedVisit.getTenantId())
                .recordedAt(Instant.now())
                .build();
        saveToOutbox("VisitNoShowEvent", savedVisit.getId(), event);

        return savedVisit;
    }

    @Override
    @Transactional
    public PropertyVisit completeVisit(UUID ownerId, UUID visitId) {
        log.info("Completing visit ID: {} by owner: {}", visitId, ownerId);
        PropertyVisit visit = propertyVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        Property property = propertyRepository.findById(visit.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (!property.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Unauthorized owner operation");
        }

        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            throw new IllegalStateException("Visit must be in SCHEDULED status to complete");
        }

        visit.setStatus(VisitStatus.COMPLETED);
        PropertyVisit savedVisit = propertyVisitRepository.save(visit);

        // Update Lead Score
        recalculateLeadScore(visit.getPropertyId(), visit.getTenantId(), property.getOwnerId());

        // Publish event to Outbox
        VisitCompletedEvent event = VisitCompletedEvent.builder()
                .visitId(savedVisit.getId())
                .propertyId(savedVisit.getPropertyId())
                .tenantId(savedVisit.getTenantId())
                .completedAt(Instant.now())
                .build();
        saveToOutbox("VisitCompletedEvent", savedVisit.getId(), event);

        return savedVisit;
    }

    @Override
    @Transactional
    public PropertyVisit cancelVisit(UUID userId, UUID visitId) {
        log.info("Cancelling visit ID: {} by user: {}", visitId, userId);
        PropertyVisit visit = propertyVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        Property property = propertyRepository.findById(visit.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!visit.getTenantId().equals(userId) && !property.getOwnerId().equals(userId)) {
            throw new SecurityException("Unauthorized cancel operation");
        }

        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            throw new IllegalStateException("Only SCHEDULED visits can be cancelled");
        }

        visit.setStatus(VisitStatus.CANCELLED);
        PropertyVisit savedVisit = propertyVisitRepository.save(visit);

        // Adjust visit slot bookings
        VisitSlot slot = visitSlotRepository.findById(visit.getVisitSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        slot.setCurrentBookings(Math.max(0, slot.getCurrentBookings() - 1));
        if (slot.getStatus() == SlotStatus.BOOKED) {
            slot.setStatus(SlotStatus.AVAILABLE);
        }

        // Check Waitlist Promotion
        List<WaitlistEntry> waitlist = waitlistEntryRepository.findByVisitSlotIdOrderByPriorityAsc(slot.getId());
        if (!waitlist.isEmpty()) {
            WaitlistEntry nextEntry = waitlist.get(0);
            nextEntry.setStatus("PROMOTED");
            waitlistEntryRepository.save(nextEntry);

            // Create new visit for promoted tenant
            PropertyVisit promotedVisit = new PropertyVisit();
            promotedVisit.setPropertyId(visit.getPropertyId());
            promotedVisit.setTenantId(nextEntry.getTenantId());
            promotedVisit.setVisitSlotId(slot.getId());
            promotedVisit.setStatus(VisitStatus.SCHEDULED);
            promotedVisit.setStartTime(slot.getStartTime());
            promotedVisit.setEndTime(slot.getEndTime());
            promotedVisit.setNotes("Automatically scheduled from waitlist");
            propertyVisitRepository.save(promotedVisit);

            // Re-increment bookings since slot was taken by waitlist promotion
            slot.setCurrentBookings(slot.getCurrentBookings() + 1);
            if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
                slot.setStatus(SlotStatus.BOOKED);
            }

            createReminder(promotedVisit);

            // Publish promoted scheduling event
            VisitScheduledEvent promoEvent = VisitScheduledEvent.builder()
                    .visitId(promotedVisit.getId())
                    .propertyId(promotedVisit.getPropertyId())
                    .tenantId(promotedVisit.getTenantId())
                    .visitSlotId(promotedVisit.getVisitSlotId())
                    .startTime(promotedVisit.getStartTime())
                    .endTime(promotedVisit.getEndTime())
                    .scheduledAt(Instant.now())
                    .build();
            saveToOutbox("VisitScheduledEvent", promotedVisit.getId(), promoEvent);
        }

        visitSlotRepository.save(slot);

        String cancelledBy = visit.getTenantId().equals(userId) ? "TENANT" : "OWNER";
        VisitCancelledEvent event = VisitCancelledEvent.builder()
                .visitId(savedVisit.getId())
                .propertyId(savedVisit.getPropertyId())
                .tenantId(savedVisit.getTenantId())
                .visitSlotId(savedVisit.getVisitSlotId())
                .cancelledBy(cancelledBy)
                .cancelledAt(Instant.now())
                .build();
        saveToOutbox("VisitCancelledEvent", savedVisit.getId(), event);

        return savedVisit;
    }

    @Override
    public List<PropertyVisit> getTenantVisits(UUID tenantId) {
        return propertyVisitRepository.findByTenantId(tenantId);
    }

    @Override
    public List<PropertyVisit> getOwnerVisits(UUID ownerId) {
        List<Property> properties = propertyRepository.findByOwnerIdAndDeletedFalse(ownerId);
        List<PropertyVisit> visits = new ArrayList<>();
        for (Property prop : properties) {
            visits.addAll(propertyVisitRepository.findByPropertyId(prop.getId()));
        }
        return visits;
    }

    @Override
    public Optional<PropertyVisit> getVisit(UUID id) {
        return propertyVisitRepository.findById(id);
    }

    private void createReminder(PropertyVisit visit) {
        Instant triggerAt = visit.getStartTime().minusSeconds(86400); // 24 hours prior
        if (triggerAt.isBefore(Instant.now())) {
            triggerAt = Instant.now();
        }

        BookingReminder reminder = new BookingReminder();
        reminder.setVisitId(visit.getId());
        reminder.setTriggerAt(triggerAt);
        reminder.setStatus("PENDING");
        reminder.setType(ReminderType.EMAIL);
        bookingReminderRepository.save(reminder);
    }

    private void qualifyLead(UUID propertyId, UUID tenantId, UUID ownerId) {
        Optional<Lead> leadOpt = leadRepository.findByPropertyIdAndTenantId(propertyId, tenantId);
        if (leadOpt.isEmpty()) {
            Lead lead = new Lead();
            lead.setPropertyId(propertyId);
            lead.setTenantId(tenantId);
            lead.setOwnerId(ownerId);
            lead.setStatus(com.roomwallah.booking.domain.entity.LeadStatus.NEW);

            LeadScoreExplanation scoreExplanation = leadScoringPort.calculateLeadScore(tenantId, ownerId);
            lead.setLeadScore(scoreExplanation.getScore());
            lead.setLeadScoreExplanation(scoreExplanation.getExplanation());

            leadRepository.save(lead);
            log.info("CRM Lead qualified and generated for tenant: {} on property: {} with score: {}", tenantId, propertyId, lead.getLeadScore());
        }
    }

    private void recalculateLeadScore(UUID propertyId, UUID tenantId, UUID ownerId) {
        Optional<Lead> leadOpt = leadRepository.findByPropertyIdAndTenantId(propertyId, tenantId);
        if (leadOpt.isPresent()) {
            Lead lead = leadOpt.get();
            LeadScoreExplanation scoreExplanation = leadScoringPort.calculateLeadScore(tenantId, ownerId);
            lead.setLeadScore(scoreExplanation.getScore());
            lead.setLeadScoreExplanation(scoreExplanation.getExplanation());
            leadRepository.save(lead);
            log.info("CRM Lead score recalculated for tenant: {} on property: {} new score: {}", tenantId, propertyId, lead.getLeadScore());
        }
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
            log.error("Failed to serialize outbox event", e);
        }
    }
}
