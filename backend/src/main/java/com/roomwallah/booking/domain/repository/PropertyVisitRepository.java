package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyVisitRepository extends JpaRepository<PropertyVisit, UUID> {
    List<PropertyVisit> findByTenantId(UUID tenantId);
    List<PropertyVisit> findByTenantIdAndStatus(UUID tenantId, VisitStatus status);
    List<PropertyVisit> findByPropertyId(UUID propertyId);
}

