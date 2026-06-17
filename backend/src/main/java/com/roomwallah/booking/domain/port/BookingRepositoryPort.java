package com.roomwallah.booking.domain.port;

import com.roomwallah.booking.domain.entity.Booking;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and retrieving Booking aggregates.
 */
public interface BookingRepositoryPort {
    Booking save(Booking booking);
    Optional<Booking> findById(UUID id);
    List<Booking> findAllByPropertyId(UUID propertyId);
    /**
     * Find bookings that conflict with a given time slot for a property.
     */
    List<Booking> findConflictingBookings(UUID propertyId, Instant start, Instant end);
}
