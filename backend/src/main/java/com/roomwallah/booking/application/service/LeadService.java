package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.Lead;
import com.roomwallah.booking.domain.entity.LeadAssignment;
import com.roomwallah.booking.domain.entity.LeadNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadService {
    Lead getOrCreateLead(UUID propertyId, UUID tenantId, UUID ownerId, String inquiryText, String phone, String email);
    LeadNote addNote(UUID leadId, UUID authorId, String content);
    LeadAssignment assignLead(UUID leadId, UUID assigneeId);
    List<Lead> getOwnerLeads(UUID ownerId);
    Optional<Lead> getLead(UUID id);
    List<LeadNote> getLeadNotes(UUID leadId);
}
