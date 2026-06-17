package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, UUID> {
    List<LeadAssignment> findByLeadId(UUID leadId);
    List<LeadAssignment> findByAssigneeId(UUID assigneeId);
}
