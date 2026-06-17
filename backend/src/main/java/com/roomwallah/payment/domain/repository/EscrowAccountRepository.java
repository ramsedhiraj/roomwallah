package com.roomwallah.payment.domain.repository;

import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowAccountRepository extends JpaRepository<EscrowAccount, UUID> {
    Optional<EscrowAccount> findByBookingId(UUID bookingId);
    List<EscrowAccount> findByStatus(EscrowStatus status);
    List<EscrowAccount> findByOwnerId(UUID ownerId);
}

