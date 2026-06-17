package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerTransaction;
import com.roomwallah.payment.domain.port.LedgerRepositoryPort;
import com.roomwallah.payment.domain.repository.LedgerEntryRepository;
import com.roomwallah.payment.domain.repository.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepositoryPort {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    @Override
    public LedgerTransaction save(LedgerTransaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public Optional<LedgerTransaction> findById(UUID id) {
        return transactionRepository.findById(id);
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        return entryRepository.save(entry);
    }

    @Override
    public Optional<LedgerEntry> findEntryById(UUID id) {
        return entryRepository.findById(id);
    }

    @Override
    public List<LedgerEntry> findEntriesByTransactionId(UUID transactionId) {
        return entryRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<LedgerEntry> findEntriesByAccountNumber(String accountNumber) {
        return entryRepository.findByAccountNumber(accountNumber);
    }
}
