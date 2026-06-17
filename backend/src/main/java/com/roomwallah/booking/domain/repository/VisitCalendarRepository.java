package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.VisitCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitCalendarRepository extends JpaRepository<VisitCalendar, UUID> {
    Optional<VisitCalendar> findByOwnerId(UUID ownerId);
}
