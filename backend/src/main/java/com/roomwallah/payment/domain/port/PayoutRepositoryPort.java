package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.Payout;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepositoryPort {
    Payout save(Payout payout);
    Optional<Payout> findById(UUID id);
    List<Payout> findByOwnerId(UUID ownerId);
}
