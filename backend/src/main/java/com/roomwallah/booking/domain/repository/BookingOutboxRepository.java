package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.BookingOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingOutboxRepository extends JpaRepository<BookingOutbox, UUID> {
    List<BookingOutbox> findByStatus(String status);
}
