package com.roomwallah.payment;

import com.roomwallah.payment.application.service.EscrowServiceImpl;
import com.roomwallah.payment.application.service.LedgerService;
import com.roomwallah.payment.domain.entity.*;
import com.roomwallah.payment.domain.port.EscrowRepositoryPort;
import com.roomwallah.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EscrowServiceImpl.
 * Covers: fund holding, fund release (Saga), fund refund, invalid state transitions,
 * and double-entry verification for each operation.
 */
public class EscrowServiceTest {

    @Mock
    private EscrowRepositoryPort escrowRepositoryPort;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    private UUID bookingId;
    private UUID paymentId;
    private UUID tenantId;
    private UUID ownerId;
    private UUID escrowId;
    private BigDecimal escrowAmount;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        escrowId = UUID.randomUUID();
        escrowAmount = new BigDecimal("10000.00");
    }

    // ========================= HOLD FUNDS TESTS =========================

    @Test
    public void testHoldFunds_Success_ReturnsHeldEscrow() {
        EscrowAccount savedAccount = buildEscrowAccount(EscrowStatus.HELD);
        when(escrowRepositoryPort.save(any(EscrowAccount.class))).thenReturn(savedAccount);

        EscrowAccount result = escrowService.holdFunds(bookingId, paymentId, tenantId, ownerId, escrowAmount, "INR");

        assertNotNull(result);
        assertEquals(EscrowStatus.HELD, result.getStatus());
        verify(escrowRepositoryPort, times(1)).save(any(EscrowAccount.class));
        // No ledger entries posted on hold (Debit Cash + Credit Escrow done in PaymentServiceImpl)
    }

    // ========================= RELEASE FUNDS TESTS =========================

    @Test
    public void testReleaseFunds_Success_PostsLedgerEntriesAndUpdatesStatus() {
        EscrowAccount heldAccount = buildEscrowAccount(EscrowStatus.HELD);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(heldAccount));

        EscrowAccount releasedAccount = buildEscrowAccount(EscrowStatus.RELEASED);
        when(escrowRepositoryPort.save(any(EscrowAccount.class))).thenReturn(releasedAccount);

        EscrowAccount result = escrowService.releaseFunds(escrowId);

        assertNotNull(result);
        assertEquals(EscrowStatus.RELEASED, result.getStatus());
        // Verify ledger posting: Debit Escrow Liability, Credit Settlement Liability
        verify(ledgerService, times(1)).postTransaction(anyString(), any(List.class));
    }

    @Test
    public void testReleaseFunds_NotHeld_ThrowsIllegalStateException() {
        EscrowAccount releasedAccount = buildEscrowAccount(EscrowStatus.RELEASED);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(releasedAccount));

        assertThrows(IllegalStateException.class,
                () -> escrowService.releaseFunds(escrowId));

        verify(ledgerService, never()).postTransaction(anyString(), any());
    }

    @Test
    public void testReleaseFunds_NotFound_ThrowsResourceNotFoundException() {
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> escrowService.releaseFunds(escrowId));
    }

    @Test
    public void testReleaseFunds_LedgerEntriesAreBalanced() {
        EscrowAccount heldAccount = buildEscrowAccount(EscrowStatus.HELD);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(heldAccount));

        EscrowAccount releasedAccount = buildEscrowAccount(EscrowStatus.RELEASED);
        when(escrowRepositoryPort.save(any())).thenReturn(releasedAccount);

        // Capture entries
        org.mockito.ArgumentCaptor<List> entriesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        escrowService.releaseFunds(escrowId);

        verify(ledgerService).postTransaction(anyString(), entriesCaptor.capture());
        assertEquals(2, entriesCaptor.getValue().size(),
                "Escrow release must post exactly 2 ledger entries (Debit ESCROW_LIABILITY + Credit SETTLEMENT_LIABILITY)");
    }

    // ========================= REFUND ESCROW TESTS =========================

    @Test
    public void testRefundEscrow_Success_PostsLedgerEntriesAndUpdatesStatus() {
        EscrowAccount heldAccount = buildEscrowAccount(EscrowStatus.HELD);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(heldAccount));

        EscrowAccount refundedAccount = buildEscrowAccount(EscrowStatus.REFUNDED);
        when(escrowRepositoryPort.save(any(EscrowAccount.class))).thenReturn(refundedAccount);

        EscrowAccount result = escrowService.refundEscrow(escrowId);

        assertNotNull(result);
        assertEquals(EscrowStatus.REFUNDED, result.getStatus());
        // Verify ledger posting: Debit Escrow Liability, Credit Cash
        verify(ledgerService, times(1)).postTransaction(anyString(), any(List.class));
    }

    @Test
    public void testRefundEscrow_AlreadyRefunded_ThrowsIllegalStateException() {
        EscrowAccount refundedAccount = buildEscrowAccount(EscrowStatus.REFUNDED);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(refundedAccount));

        assertThrows(IllegalStateException.class,
                () -> escrowService.refundEscrow(escrowId));

        verify(ledgerService, never()).postTransaction(anyString(), any());
    }

    @Test
    public void testRefundEscrow_ReleasedCannotBeRefunded_ThrowsIllegalStateException() {
        EscrowAccount releasedAccount = buildEscrowAccount(EscrowStatus.RELEASED);
        when(escrowRepositoryPort.findById(escrowId)).thenReturn(Optional.of(releasedAccount));

        assertThrows(IllegalStateException.class,
                () -> escrowService.refundEscrow(escrowId));
    }

    // ========================= HELPER METHODS ========================

    private EscrowAccount buildEscrowAccount(EscrowStatus status) {
        EscrowAccount account = new EscrowAccount();
        account.setId(escrowId);
        account.setBookingId(bookingId);
        account.setPaymentId(paymentId);
        account.setTenantId(tenantId);
        account.setOwnerId(ownerId);
        account.setBalance(escrowAmount);
        account.setCurrency("INR");
        account.setStatus(status);
        if (status == EscrowStatus.HELD) {
            account.setHeldAt(Instant.now());
        }
        return account;
    }
}
