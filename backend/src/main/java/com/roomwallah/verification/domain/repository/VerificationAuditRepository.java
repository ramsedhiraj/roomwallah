package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.VerificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationAuditRepository extends JpaRepository<VerificationAudit, UUID> {
    List<VerificationAudit> findByVerificationIdOrderByTimestampDesc(UUID verificationId);
}
