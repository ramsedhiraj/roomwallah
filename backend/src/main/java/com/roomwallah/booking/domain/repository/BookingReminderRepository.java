package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.BookingReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingReminderRepository extends JpaRepository<BookingReminder, UUID> {
    List<BookingReminder> findByStatus(String status);
    List<BookingReminder> findByStatusAndTriggerAtBefore(String status, Instant time);
}
