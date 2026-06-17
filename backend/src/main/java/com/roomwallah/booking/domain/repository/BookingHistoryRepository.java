package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, UUID> {
    List<BookingHistory> findByBookingId(UUID bookingId);
}
