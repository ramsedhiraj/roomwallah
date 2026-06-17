package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.DisputeStatus;
import com.roomwallah.payment.domain.entity.PaymentDispute;
import com.roomwallah.payment.domain.port.DisputeRepositoryPort;
import com.roomwallah.payment.domain.repository.PaymentDisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DisputeRepositoryAdapter implements DisputeRepositoryPort {

    private final PaymentDisputeRepository disputeRepository;

    @Override
    public PaymentDispute save(PaymentDispute dispute) {
        return disputeRepository.save(dispute);
    }

    @Override
    public Optional<PaymentDispute> findById(UUID id) {
        return disputeRepository.findById(id);
    }

    @Override
    public List<PaymentDispute> findByPaymentId(UUID paymentId) {
        return disputeRepository.findByPaymentId(paymentId);
    }

    @Override
    public List<PaymentDispute> findAll() {
        return disputeRepository.findAll();
    }

    @Override
    public long countOpen() {
        return disputeRepository.countByStatusIn(java.util.List.of(DisputeStatus.OPEN));
    }
}

