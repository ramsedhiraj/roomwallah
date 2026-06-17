package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.Lead;
import com.roomwallah.booking.domain.entity.LeadActivity;
import com.roomwallah.booking.domain.entity.LeadAssignment;
import com.roomwallah.booking.domain.entity.LeadNote;
import com.roomwallah.booking.domain.entity.LeadStatus;
import com.roomwallah.booking.domain.event.LeadCreatedEvent;
import com.roomwallah.booking.domain.port.LeadScoringPort;
import com.roomwallah.booking.domain.repository.LeadActivityRepository;
import com.roomwallah.booking.domain.repository.LeadAssignmentRepository;
import com.roomwallah.booking.domain.repository.LeadNoteRepository;
import com.roomwallah.booking.domain.repository.LeadRepository;
import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final UserRepository userRepository;
    private final LeadScoringPort leadScoringPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Lead getOrCreateLead(UUID propertyId, UUID tenantId, UUID ownerId, String inquiryText, String phone, String email) {
        log.info("Get or create lead for tenant: {} on property: {}", tenantId, propertyId);
        Optional<Lead> leadOpt = leadRepository.findByPropertyIdAndTenantId(propertyId, tenantId);

        if (leadOpt.isPresent()) {
            return leadOpt.get();
        }

        Lead lead = new Lead();
        lead.setPropertyId(propertyId);
        lead.setTenantId(tenantId);
        lead.setOwnerId(ownerId);
        lead.setStatus(LeadStatus.NEW);
        lead.setInquiryText(inquiryText);
        lead.setContactPhone(phone);
        lead.setContactEmail(email);

        // Calculate lead score
        LeadScoreExplanation scoreExplanation = leadScoringPort.calculateLeadScore(tenantId, ownerId);
        lead.setLeadScore(scoreExplanation.getScore());
        lead.setLeadScoreExplanation(scoreExplanation.getExplanation());

        Lead savedLead = leadRepository.save(lead);

        // Record Activity
        recordActivity(savedLead.getId(), "LEAD_CREATED", "Lead generated automatically through inquiry/visit");

        // Trigger automatic round-robin assignment to admins
        autoAssignLead(savedLead.getId());
        
        // Notify Owner
        eventPublisher.publishEvent(new LeadCreatedEvent(savedLead.getId(), ownerId, propertyId));

        return savedLead;
    }

    @Override
    @Transactional
    public LeadNote addNote(UUID leadId, UUID authorId, String content) {
        log.info("Adding lead note to lead: {} by author: {}", leadId, authorId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        LeadNote note = new LeadNote();
        note.setLeadId(leadId);
        note.setAuthorId(authorId);
        note.setContent(content);

        LeadNote savedNote = leadNoteRepository.save(note);

        recordActivity(leadId, "NOTE_ADDED", "New note added by: " + authorId);

        return savedNote;
    }

    @Override
    @Transactional
    public LeadAssignment assignLead(UUID leadId, UUID assigneeId) {
        log.info("Assigning lead: {} to assignee: {}", leadId, assigneeId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));

        LeadAssignment assignment = new LeadAssignment();
        assignment.setLeadId(leadId);
        assignment.setAssigneeId(assigneeId);
        assignment.setStatus("ASSIGNED");
        assignment.setAssignedAt(Instant.now());

        LeadAssignment savedAssignment = leadAssignmentRepository.save(assignment);

        // Transition Lead status
        lead.setStatus(LeadStatus.CONTACTED);
        leadRepository.save(lead);

        recordActivity(leadId, "LEAD_ASSIGNED", "Lead manually assigned to: " + assignee.getFullName());

        return savedAssignment;
    }

    @Override
    public List<Lead> getOwnerLeads(UUID ownerId) {
        return leadRepository.findByOwnerId(ownerId);
    }

    @Override
    public Optional<Lead> getLead(UUID id) {
        return leadRepository.findById(id);
    }

    @Override
    public List<LeadNote> getLeadNotes(UUID leadId) {
        return leadNoteRepository.findByLeadId(leadId);
    }

    private void autoAssignLead(UUID leadId) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .toList();

        if (admins.isEmpty()) {
            log.info("No admins available for auto-assigning lead: {}", leadId);
            return;
        }

        // Sequential round-robin assignment using lead assignment table count modulo list size
        long totalAssignments = leadAssignmentRepository.count();
        int index = (int) (totalAssignments % admins.size());
        User assignee = admins.get(index);

        LeadAssignment assignment = new LeadAssignment();
        assignment.setLeadId(leadId);
        assignment.setAssigneeId(assignee.getId());
        assignment.setStatus("AUTO_ASSIGNED");
        assignment.setAssignedAt(Instant.now());
        leadAssignmentRepository.save(assignment);

        recordActivity(leadId, "LEAD_AUTO_ASSIGNED", "Lead auto-assigned to admin: " + assignee.getFullName());
    }

    private void recordActivity(UUID leadId, String activityType, String description) {
        LeadActivity activity = new LeadActivity();
        activity.setLeadId(leadId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        leadActivityRepository.save(activity);
    }
}
