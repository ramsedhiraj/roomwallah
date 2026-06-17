package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.Payment;
import com.roomwallah.payment.domain.port.PaymentRepositoryPort;
import com.roomwallah.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Payment> findByTenantId(UUID tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }
}

