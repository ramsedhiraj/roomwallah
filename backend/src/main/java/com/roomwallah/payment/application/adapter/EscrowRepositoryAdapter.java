package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.EscrowStatus;
import com.roomwallah.payment.domain.port.EscrowRepositoryPort;
import com.roomwallah.payment.domain.repository.EscrowAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EscrowRepositoryAdapter implements EscrowRepositoryPort {

    private final EscrowAccountRepository escrowAccountRepository;

    @Override
    public EscrowAccount save(EscrowAccount escrowAccount) {
        return escrowAccountRepository.save(escrowAccount);
    }

    @Override
    public Optional<EscrowAccount> findById(UUID id) {
        return escrowAccountRepository.findById(id);
    }

    @Override
    public Optional<EscrowAccount> findByBookingId(UUID bookingId) {
        return escrowAccountRepository.findByBookingId(bookingId);
    }

    @Override
    public List<EscrowAccount> findByStatus(EscrowStatus status) {
        return escrowAccountRepository.findByStatus(status);
    }

    @Override
    public List<EscrowAccount> findByOwnerId(UUID ownerId) {
        return escrowAccountRepository.findByOwnerId(ownerId);
    }
}

