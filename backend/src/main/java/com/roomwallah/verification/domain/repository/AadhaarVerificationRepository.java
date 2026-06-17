package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.AadhaarVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AadhaarVerificationRepository extends JpaRepository<AadhaarVerification, UUID> {
    Optional<AadhaarVerification> findByUserId(UUID userId);
}
