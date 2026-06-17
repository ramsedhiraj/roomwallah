package com.roomwallah.payment.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerEntryType;
import com.roomwallah.payment.domain.entity.Payout;
import com.roomwallah.payment.domain.entity.PayoutStatus;
import com.roomwallah.payment.domain.port.PayoutRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepositoryPort payoutRepositoryPort;
    private final LedgerService ledgerService;
    private final PaymentLockService paymentLockService;

    @Override
    @Transactional
    public Payout initiatePayout(UUID ownerId, BigDecimal amount, String destinationAccount) {
        log.info("Initiating payout for owner: {}, amount: {}, destination: {}", ownerId, amount, destinationAccount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payout amount must be positive");
        }

        Payout payout = new Payout();
        payout.setOwnerId(ownerId);
        payout.setAmount(amount);
        payout.setCurrency("INR");
        payout.setStatus(PayoutStatus.PENDING);
        payout.setDestinationAccount(destinationAccount);

        Payout saved = payoutRepositoryPort.save(payout);
        log.info("Payout initiated successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Payout settlePayout(UUID payoutId, String gatewayPayoutId) {
        String lockKey = "lock:payout:settle:" + payoutId;

        return paymentLockService.executeWithLock(lockKey, 30, () -> {
            log.info("Settling payout ID: {}, gateway payout ID: {}", payoutId, gatewayPayoutId);

            Payout payout = payoutRepositoryPort.findById(payoutId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payout not found with ID: " + payoutId));

            if (payout.getStatus() == PayoutStatus.SUCCEEDED) {
                log.warn("Payout ID: {} is already settled/SUCCEEDED", payoutId);
                return payout;
            }

            payout.setStatus(PayoutStatus.SUCCEEDED);
            payout.setGatewayPayoutId(gatewayPayoutId);
            Payout saved = payoutRepositoryPort.save(payout);

            // Record double-entry ledger transaction: Debit Settlement Liability, Credit Cash
            LedgerEntry debit = new LedgerEntry();
            debit.setAccountNumber("SETTLEMENT_LIABILITY");
            debit.setEntryType(LedgerEntryType.DEBIT);
            debit.setAmount(saved.getAmount());
            debit.setCurrency(saved.getCurrency());

            LedgerEntry credit = new LedgerEntry();
            credit.setAccountNumber("CASH");
            credit.setEntryType(LedgerEntryType.CREDIT);
            credit.setAmount(saved.getAmount());
            credit.setCurrency(saved.getCurrency());

            ledgerService.postTransaction(
                    "Settle payout to owner account for owner ID: " + saved.getOwnerId(),
                    List.of(debit, credit)
            );

            log.info("Payout ID: {} settled successfully", payoutId);
            return saved;
        });
    }
}
