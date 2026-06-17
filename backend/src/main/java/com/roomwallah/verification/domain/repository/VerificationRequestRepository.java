package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.entity.VerificationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
    Optional<VerificationRequest> findByIdempotencyKey(String idempotencyKey);
    List<VerificationRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<VerificationRequest> findByRequestStatusOrderByCreatedAtDesc(VerificationRequestStatus status);
    Optional<VerificationRequest> findFirstByUserIdOrderByVerificationVersionDesc(UUID userId);
    Optional<VerificationRequest> findFirstByUserIdAndRequestStatusOrderByCreatedAtDesc(UUID userId, VerificationRequestStatus status);
    List<VerificationRequest> findByRequestStatusAndExpiresAtBefore(VerificationRequestStatus status, java.time.Instant time);
    List<VerificationRequest> findByIdempotencyCleanupAfterBeforeAndIdempotencyKeyIsNotNull(java.time.Instant time);
}
