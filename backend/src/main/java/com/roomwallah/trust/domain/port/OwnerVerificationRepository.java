package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.OwnerVerification;
import com.roomwallah.trust.domain.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, UUID> {
    Optional<OwnerVerification> findByUserId(UUID userId);
    Optional<OwnerVerification> findByIdempotencyKey(String idempotencyKey);
    List<OwnerVerification> findByVerificationStatus(VerificationStatus status);
    List<OwnerVerification> findByVerificationStatusAndExpiresAtBefore(VerificationStatus status, Instant now);
}
