package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.VerificationDecisionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationDecisionAuditRepository extends JpaRepository<VerificationDecisionAudit, UUID> {
    List<VerificationDecisionAudit> findByVerificationRequestIdOrderByCreatedAtAsc(UUID verificationRequestId);
}
