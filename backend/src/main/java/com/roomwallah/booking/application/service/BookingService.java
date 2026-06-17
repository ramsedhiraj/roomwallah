package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingService {
    Booking createBooking(UUID tenantId, BookingRequestDto request);
    Booking approveBooking(UUID ownerId, UUID bookingId);
    Booking rejectBooking(UUID ownerId, UUID bookingId, String reason);
    Booking cancelBooking(UUID userId, UUID bookingId);
    List<Booking> getTenantBookings(UUID tenantId);
    List<Booking> getOwnerBookings(UUID ownerId);
    Optional<Booking> getBooking(UUID id);
    void expireUnconfirmedBookings();
}
