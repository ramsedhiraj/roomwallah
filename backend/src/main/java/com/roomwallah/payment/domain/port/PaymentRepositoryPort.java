package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByTenantId(UUID tenantId);
    List<Payment> findAll();
}
