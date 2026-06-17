package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.UserVerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVerificationLogRepository extends JpaRepository<UserVerificationLog, UUID> {
    List<UserVerificationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<UserVerificationLog> findFirstByUserIdAndVerificationTypeOrderByCreatedAtDesc(UUID userId, String verificationType);
}
