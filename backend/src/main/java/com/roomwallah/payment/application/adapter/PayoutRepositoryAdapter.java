package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.Payout;
import com.roomwallah.payment.domain.port.PayoutRepositoryPort;
import com.roomwallah.payment.domain.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayoutRepositoryAdapter implements PayoutRepositoryPort {

    private final PayoutRepository payoutRepository;

    @Override
    public Payout save(Payout payout) {
        return payoutRepository.save(payout);
    }

    @Override
    public Optional<Payout> findById(UUID id) {
        return payoutRepository.findById(id);
    }

    @Override
    public List<Payout> findByOwnerId(UUID ownerId) {
        return payoutRepository.findByOwnerId(ownerId);
    }
}
