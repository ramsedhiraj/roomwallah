package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, UUID> {
    Optional<PropertyVerification> findByPropertyId(UUID propertyId);
    List<PropertyVerification> findByOwnerId(UUID ownerId);
    List<PropertyVerification> findByApprovalStatus(String approvalStatus);
}
