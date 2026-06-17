package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.Refund;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findById(UUID id);
    List<Refund> findByPaymentId(UUID paymentId);
}
