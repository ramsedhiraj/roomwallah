package com.roomwallah.agreement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, UUID> {

    @Override
    @EntityGraph(attributePaths = {"signatures"})
    Optional<LeaseAgreement> findById(UUID id);

    @EntityGraph(attributePaths = {"signatures"})
    List<LeaseAgreement> findByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {"signatures"})
    List<LeaseAgreement> findByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = {"signatures"})
    List<LeaseAgreement> findByPropertyId(UUID propertyId);
}
