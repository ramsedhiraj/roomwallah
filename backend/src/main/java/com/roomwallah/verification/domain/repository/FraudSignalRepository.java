package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudSignalRepository extends JpaRepository<FraudSignal, UUID> {
    List<FraudSignal> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<FraudSignal> findAllByOrderByCreatedAtDesc();
}
