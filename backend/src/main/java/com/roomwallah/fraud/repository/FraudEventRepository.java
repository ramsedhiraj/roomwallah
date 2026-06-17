package com.roomwallah.fraud.repository;

import com.roomwallah.fraud.domain.FraudEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudEventRepository extends JpaRepository<FraudEvent, UUID> {
    List<FraudEvent> findByUserId(UUID userId);
}
