package com.roomwallah.payment.domain.repository;

import com.roomwallah.payment.domain.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    List<Payout> findByOwnerId(UUID ownerId);
}
