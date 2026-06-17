package com.roomwallah.payment.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.payment.domain.entity.EscrowAccount;
import com.roomwallah.payment.domain.entity.EscrowStatus;
import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerEntryType;
import com.roomwallah.payment.domain.port.EscrowRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final EscrowRepositoryPort escrowRepositoryPort;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    public EscrowAccount holdFunds(UUID bookingId, UUID paymentId, UUID tenantId, UUID ownerId, BigDecimal amount, String currency) {
        log.info("Holding funds in escrow for booking: {}, payment: {}, amount: {}", bookingId, paymentId, amount);

        EscrowAccount escrowAccount = new EscrowAccount();
        escrowAccount.setBookingId(bookingId);
        escrowAccount.setPaymentId(paymentId);
        escrowAccount.setTenantId(tenantId);
        escrowAccount.setOwnerId(ownerId);
        escrowAccount.setBalance(amount);
        escrowAccount.setCurrency(currency != null ? currency : "INR");
        escrowAccount.setStatus(EscrowStatus.HELD);
        escrowAccount.setHeldAt(Instant.now());

        EscrowAccount saved = escrowRepositoryPort.save(escrowAccount);
        log.info("Escrow account created successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public EscrowAccount releaseFunds(UUID escrowAccountId) {
        log.info("Releasing funds from escrow account: {}", escrowAccountId);
        EscrowAccount escrow = escrowRepositoryPort.findById(escrowAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow account not found with ID: " + escrowAccountId));

        if (escrow.getStatus() != EscrowStatus.HELD) {
            throw new IllegalStateException("Escrow account is not in HELD status. Current status: " + escrow.getStatus());
        }

        BigDecimal releaseAmount = escrow.getBalance();
        String currency = escrow.getCurrency();

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setReleasedAt(Instant.now());
        escrow.setBalance(BigDecimal.ZERO);
        EscrowAccount saved = escrowRepositoryPort.save(escrow);

        // Record double-entry ledger transaction: Debit Escrow Liability, Credit Settlement Liability
        LedgerEntry debit = new LedgerEntry();
        debit.setAccountNumber("ESCROW_LIABILITY");
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setAmount(releaseAmount);
        debit.setCurrency(currency);

        LedgerEntry credit = new LedgerEntry();
        credit.setAccountNumber("SETTLEMENT_LIABILITY");
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(releaseAmount);
        credit.setCurrency(currency);

        ledgerService.postTransaction(
                "Release escrow funds to owner for booking ID: " + escrow.getBookingId(),
                List.of(debit, credit)
        );

        log.info("Escrow funds released successfully for account ID: {}", escrowAccountId);
        return saved;
    }

    @Override
    @Transactional
    public EscrowAccount refundEscrow(UUID escrowAccountId) {
        log.info("Refund initiated from escrow account: {}", escrowAccountId);
        EscrowAccount escrow = escrowRepositoryPort.findById(escrowAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow account not found with ID: " + escrowAccountId));

        if (escrow.getStatus() != EscrowStatus.HELD) {
            throw new IllegalStateException("Escrow account is not in HELD status. Current status: " + escrow.getStatus());
        }

        BigDecimal refundAmount = escrow.getBalance();
        String currency = escrow.getCurrency();

        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setBalance(BigDecimal.ZERO);
        EscrowAccount saved = escrowRepositoryPort.save(escrow);

        // Record double-entry ledger transaction: Debit Escrow Liability, Credit Cash
        LedgerEntry debit = new LedgerEntry();
        debit.setAccountNumber("ESCROW_LIABILITY");
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setAmount(refundAmount);
        debit.setCurrency(currency);

        LedgerEntry credit = new LedgerEntry();
        credit.setAccountNumber("CASH");
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(refundAmount);
        credit.setCurrency(currency);

        ledgerService.postTransaction(
                "Refund escrow funds to tenant for booking ID: " + escrow.getBookingId(),
                List.of(debit, credit)
        );

        log.info("Escrow funds refunded successfully for account ID: {}", escrowAccountId);
        return saved;
    }
}
