package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
    List<WaitlistEntry> findByVisitSlotIdOrderByPriorityAsc(UUID visitSlotId);
}
