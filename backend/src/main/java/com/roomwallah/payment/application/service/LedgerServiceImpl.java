package com.roomwallah.payment.application.service;

import com.roomwallah.payment.application.exception.BalancedLedgerException;
import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerEntryType;
import com.roomwallah.payment.domain.entity.LedgerTransaction;
import com.roomwallah.payment.domain.port.LedgerRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepositoryPort ledgerRepositoryPort;

    @Override
    @Transactional
    public void postTransaction(String description, List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Ledger transaction must contain at least one entry");
        }
        log.info("Posting ledger transaction: {}, entries count: {}", description, entries.size());

        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Ledger entry amount must be positive");
            }
            if (entry.getEntryType() == null) {
                throw new IllegalArgumentException("Ledger entry type (DEBIT/CREDIT) must be specified");
            }
            if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                debitSum = debitSum.add(entry.getAmount());
            } else {
                creditSum = creditSum.add(entry.getAmount());
            }
        }

        if (debitSum.compareTo(creditSum) != 0) {
            log.error("Unbalanced ledger transaction. Debits: {}, Credits: {}", debitSum, creditSum);
            throw new BalancedLedgerException("Ledger transaction is unbalanced. Debits sum (" + debitSum 
                    + ") must equal Credits sum (" + creditSum + ")");
        }

        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setDescription(description);
        transaction.setPostedAt(Instant.now());

        for (LedgerEntry entry : entries) {
            transaction.addEntry(entry);
        }

        ledgerRepositoryPort.save(transaction);
        log.info("Ledger transaction posted successfully with ID: {}", transaction.getId());
    }
}
