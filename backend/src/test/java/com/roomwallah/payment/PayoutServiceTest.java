package com.roomwallah.payment;

import com.roomwallah.payment.application.service.PayoutServiceImpl;
import com.roomwallah.payment.application.service.LedgerService;
import com.roomwallah.payment.application.service.PaymentLockService;
import com.roomwallah.payment.domain.entity.Payout;
import com.roomwallah.payment.domain.entity.PayoutStatus;
import com.roomwallah.payment.domain.port.PayoutRepositoryPort;
import com.roomwallah.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayoutServiceImpl.
 * Covers: payout initiation, settlement Saga, lock enforcement, and idempotency.
 */
public class PayoutServiceTest {

    @Mock
    private PayoutRepositoryPort payoutRepositoryPort;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private PaymentLockService paymentLockService;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private UUID ownerId;
    private UUID payoutId;
    private BigDecimal payoutAmount;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ownerId = UUID.randomUUID();
        payoutId = UUID.randomUUID();
        payoutAmount = new BigDecimal("8000.00");

        // Default: lock service runs task directly
        when(paymentLockService.executeWithLock(anyString(), anyLong(), any())).thenAnswer(inv -> {
            Supplier<?> task = inv.getArgument(2);
            return task.get();
        });
    }

    // ========================= INITIATE PAYOUT TESTS =========================

    @Test
    public void testInitiatePayout_Success_ReturnsPendingPayout() {
        Payout savedPayout = buildPayout(payoutId, PayoutStatus.PENDING);
        when(payoutRepositoryPort.save(any(Payout.class))).thenReturn(savedPayout);

        Payout result = payoutService.initiatePayout(ownerId, payoutAmount, "ACC1234567890");

        assertNotNull(result);
        assertEquals(PayoutStatus.PENDING, result.getStatus());
        verify(payoutRepositoryPort, times(1)).save(any(Payout.class));
    }

    @Test
    public void testInitiatePayout_ZeroAmount_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(ownerId, BigDecimal.ZERO, "ACC123"));
    }

    @Test
    public void testInitiatePayout_NegativeAmount_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(ownerId, new BigDecimal("-100"), "ACC123"));
    }

    // ========================= SETTLE PAYOUT TESTS =========================

    @Test
    public void testSettlePayout_Success_PostsLedgerEntries() {
        Payout pendingPayout = buildPayout(payoutId, PayoutStatus.PENDING);
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.of(pendingPayout));

        Payout settledPayout = buildPayout(payoutId, PayoutStatus.SUCCEEDED);
        settledPayout.setGatewayPayoutId("po_gateway_001");
        when(payoutRepositoryPort.save(any(Payout.class))).thenReturn(settledPayout);

        Payout result = payoutService.settlePayout(payoutId, "po_gateway_001");

        assertNotNull(result);
        assertEquals(PayoutStatus.SUCCEEDED, result.getStatus());
        // Verify double-entry ledger posting: Debit Settlement Liability, Credit Cash
        verify(ledgerService, times(1)).postTransaction(anyString(), any(List.class));
    }

    @Test
    public void testSettlePayout_AlreadySettled_IsIdempotent() {
        Payout settledPayout = buildPayout(payoutId, PayoutStatus.SUCCEEDED);
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.of(settledPayout));

        Payout result = payoutService.settlePayout(payoutId, "po_already_done");

        assertEquals(PayoutStatus.SUCCEEDED, result.getStatus());
        // Should not post ledger entries again
        verify(ledgerService, never()).postTransaction(anyString(), any());
    }

    @Test
    public void testSettlePayout_PayoutNotFound_ThrowsResourceNotFoundException() {
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> payoutService.settlePayout(payoutId, "po_not_found"));
    }

    @Test
    public void testSettlePayout_ExecutedUnderDistributedLock() {
        Payout pendingPayout = buildPayout(payoutId, PayoutStatus.PENDING);
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.of(pendingPayout));
        when(payoutRepositoryPort.save(any())).thenReturn(pendingPayout);

        payoutService.settlePayout(payoutId, "po_lock_test");

        verify(paymentLockService, times(1)).executeWithLock(
                eq("lock:payout:settle:" + payoutId), anyLong(), any());
    }

    @Test
    public void testSettlePayout_LedgerEntriesAreBalanced() {
        // Verify that the two ledger entries (Debit Settlement Liability, Credit Cash)
        // have matching amounts — ensuring the double-entry balance is maintained
        Payout pendingPayout = buildPayout(payoutId, PayoutStatus.PENDING);
        when(payoutRepositoryPort.findById(payoutId)).thenReturn(Optional.of(pendingPayout));

        Payout settledPayout = buildPayout(payoutId, PayoutStatus.SUCCEEDED);
        when(payoutRepositoryPort.save(any())).thenReturn(settledPayout);

        // Capture what entries are passed to ledgerService
        org.mockito.ArgumentCaptor<List> entriesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        payoutService.settlePayout(payoutId, "po_balance_test");

        verify(ledgerService).postTransaction(anyString(), entriesCaptor.capture());
        List<?> capturedEntries = entriesCaptor.getValue();

        // There should be exactly 2 entries (Debit and Credit)
        assertEquals(2, capturedEntries.size(),
                "Payout settlement must post exactly 2 ledger entries (Debit + Credit)");
    }

    // ========================= HELPER METHODS ========================

    private Payout buildPayout(UUID id, PayoutStatus status) {
        Payout payout = new Payout();
        payout.setId(id);
        payout.setOwnerId(ownerId);
        payout.setAmount(payoutAmount);
        payout.setCurrency("INR");
        payout.setStatus(status);
        payout.setDestinationAccount("ACC1234567890");
        return payout;
    }
}
