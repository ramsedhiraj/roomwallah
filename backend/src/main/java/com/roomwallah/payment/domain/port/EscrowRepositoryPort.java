package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.EscrowStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscrowRepositoryPort {
    EscrowAccount save(EscrowAccount escrowAccount);
    Optional<EscrowAccount> findById(UUID id);
    Optional<EscrowAccount> findByBookingId(UUID bookingId);
    List<EscrowAccount> findByStatus(EscrowStatus status);
    List<EscrowAccount> findByOwnerId(UUID ownerId);
}
