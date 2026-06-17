package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.Refund;
import com.roomwallah.payment.domain.port.RefundRepositoryPort;
import com.roomwallah.payment.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepositoryPort {

    private final RefundRepository refundRepository;

    @Override
    public Refund save(Refund refund) {
        return refundRepository.save(refund);
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return refundRepository.findById(id);
    }

    @Override
    public List<Refund> findByPaymentId(UUID paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }
}
