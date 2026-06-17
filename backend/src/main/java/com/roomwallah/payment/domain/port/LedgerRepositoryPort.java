package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepositoryPort {
    LedgerTransaction save(LedgerTransaction transaction);
    Optional<LedgerTransaction> findById(UUID id);
    LedgerEntry saveEntry(LedgerEntry entry);
    Optional<LedgerEntry> findEntryById(UUID id);
    List<LedgerEntry> findEntriesByTransactionId(UUID transactionId);
    List<LedgerEntry> findEntriesByAccountNumber(String accountNumber);
}
