package com.roomwallah.fraud.repository;

import com.roomwallah.fraud.domain.FraudCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {
    List<FraudCase> findByStatus(String status);
    List<FraudCase> findByUserId(UUID userId);
}
