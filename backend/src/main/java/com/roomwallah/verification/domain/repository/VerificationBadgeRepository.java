package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.VerificationBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationBadgeRepository extends JpaRepository<VerificationBadge, UUID> {
    Optional<VerificationBadge> findByUserId(UUID userId);
}
