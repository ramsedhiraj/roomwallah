package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
    List<LeadActivity> findByLeadId(UUID leadId);
}
