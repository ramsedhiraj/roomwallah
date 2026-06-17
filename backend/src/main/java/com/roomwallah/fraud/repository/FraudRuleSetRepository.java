package com.roomwallah.fraud.repository;

import com.roomwallah.fraud.domain.FraudRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudRuleSetRepository extends JpaRepository<FraudRuleSet, UUID> {
    Optional<FraudRuleSet> findByVersionName(String name);
}
