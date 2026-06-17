package com.roomwallah.booking.domain.repository;

import com.roomwallah.booking.domain.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    List<Lead> findByOwnerId(UUID ownerId);
    List<Lead> findByTenantId(UUID tenantId);
    List<Lead> findByPropertyId(UUID propertyId);
    Optional<Lead> findByPropertyIdAndTenantId(UUID propertyId, UUID tenantId);
}

