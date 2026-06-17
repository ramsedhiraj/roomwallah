package com.roomwallah.payment.domain.repository;

import com.roomwallah.payment.domain.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByOwnerId(UUID ownerId);
    List<Settlement> findByPayoutId(UUID payoutId);
}
