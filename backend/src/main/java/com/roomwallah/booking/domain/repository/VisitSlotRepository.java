package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.VisitSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitSlotRepository extends JpaRepository<VisitSlot, UUID> {
    List<VisitSlot> findByPropertyId(UUID propertyId);
    List<VisitSlot> findByPropertyIdAndStartTimeAfter(UUID propertyId, Instant time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vs FROM VisitSlot vs WHERE vs.id = :id")
    Optional<VisitSlot> findAndLockById(@Param("id") UUID id);
}

