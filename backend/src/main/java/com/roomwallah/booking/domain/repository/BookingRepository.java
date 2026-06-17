package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByTenantId(UUID tenantId);
    List<Booking> findByOwnerId(UUID ownerId);
    List<Booking> findByPropertyId(UUID propertyId);
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
}

