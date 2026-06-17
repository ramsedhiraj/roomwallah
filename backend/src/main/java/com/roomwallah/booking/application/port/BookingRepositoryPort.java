package com.roomwallah.booking.application.port;

import com.roomwallah.booking.domain.entity.Booking;
import java.util.Optional;

/**
 * Port for persisting and retrieving Booking aggregates.
 */
public interface BookingRepositoryPort {
    Booking save(Booking booking);
    Optional<Booking> findById(java.util.UUID id);
    void delete(Booking booking);
    // Additional query methods can be added as needed, e.g., find by property, user, status.
}
