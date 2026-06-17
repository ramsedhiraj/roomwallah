package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrustScoreRepository extends JpaRepository<TrustScore, UUID> {
    Optional<TrustScore> findByUserId(UUID userId);
}
