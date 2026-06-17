package com.roomwallah.payment;

import com.roomwallah.payment.application.exception.BalancedLedgerException;
import com.roomwallah.payment.application.service.LedgerServiceImpl;
import com.roomwallah.payment.domain.entity.LedgerEntry;
import com.roomwallah.payment.domain.entity.LedgerEntryType;
import com.roomwallah.payment.domain.entity.LedgerTransaction;
import com.roomwallah.payment.domain.port.LedgerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the double-entry bookkeeping ledger service.
 * Verifies: balanced debit=credit enforcement, append-only semantics,
 * and rejection of malformed entries.
 */
public class LedgerServiceTest {

    @Mock
    private LedgerRepositoryPort ledgerRepositoryPort;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Make save return the passed transaction
        when(ledgerRepositoryPort.save(any(LedgerTransaction.class))).thenAnswer(inv -> {
            LedgerTransaction tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
    }

    // ========================= BALANCED ENTRY TESTS =========================

    @Test
    public void testPostTransaction_BalancedEntries_Success() {
        // GIVEN: balanced debit and credit of 10,000 INR
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("10000.00"));
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("10000.00"));

        // WHEN
        assertDoesNotThrow(() -> ledgerService.postTransaction("Capture payment to escrow", List.of(debit, credit)));

        // THEN: exactly one transaction was persisted
        verify(ledgerRepositoryPort, times(1)).save(any(LedgerTransaction.class));
    }

    @Test
    public void testPostTransaction_UnbalancedEntries_ThrowsBalancedLedgerException() {
        // GIVEN: debit 10,000 but credit only 9,000 — intentionally imbalanced
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("10000.00"));
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("9000.00"));

        // WHEN / THEN
        BalancedLedgerException ex = assertThrows(BalancedLedgerException.class,
                () -> ledgerService.postTransaction("Unbalanced transaction", List.of(debit, credit)));

        assertTrue(ex.getMessage().contains("unbalanced"), "Exception message should mention 'unbalanced'");
        // Verify nothing was persisted
        verify(ledgerRepositoryPort, never()).save(any());
    }

    @Test
    public void testPostTransaction_MultipleBalancedEntries_Success() {
        // GIVEN: 3 debits and 3 credits each summing to 30,000
        LedgerEntry d1 = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("10000.00"));
        LedgerEntry d2 = buildEntry(LedgerEntryType.DEBIT, "PLATFORM_FEE", new BigDecimal("500.00"));
        LedgerEntry d3 = buildEntry(LedgerEntryType.DEBIT, "GST", new BigDecimal("90.00"));
        BigDecimal totalDebit = new BigDecimal("10590.00");

        LedgerEntry c1 = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("10000.00"));
        LedgerEntry c2 = buildEntry(LedgerEntryType.CREDIT, "PLATFORM_REVENUE", new BigDecimal("500.00"));
        LedgerEntry c3 = buildEntry(LedgerEntryType.CREDIT, "TAX_PAYABLE", new BigDecimal("90.00"));

        // WHEN / THEN
        assertDoesNotThrow(() ->
                ledgerService.postTransaction("Multi-line booking capture", List.of(d1, d2, d3, c1, c2, c3)));
        verify(ledgerRepositoryPort, times(1)).save(any(LedgerTransaction.class));
    }

    @Test
    public void testPostTransaction_EmptyEntries_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.postTransaction("Empty transaction", List.of()));
    }

    @Test
    public void testPostTransaction_NullEntries_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.postTransaction("Null entries", null));
    }

    @Test
    public void testPostTransaction_ZeroAmountEntry_ThrowsIllegalArgumentException() {
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", BigDecimal.ZERO);
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.postTransaction("Zero amount", List.of(debit, credit)));
    }

    @Test
    public void testPostTransaction_NegativeAmountEntry_ThrowsIllegalArgumentException() {
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("-100.00"));
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("-100.00"));

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.postTransaction("Negative amount", List.of(debit, credit)));
    }

    @Test
    public void testPostTransaction_NullEntryType_ThrowsIllegalArgumentException() {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountNumber("CASH");
        entry.setAmount(new BigDecimal("1000.00"));
        entry.setEntryType(null); // missing type

        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("1000.00"));

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.postTransaction("Null type", List.of(entry, credit)));
    }

    // ========================= IMMUTABILITY GUARD TESTS ========================

    @Test
    public void testPostTransaction_AppendOnly_NoUpdateOrDelete() {
        // The ledger service must never call any update/delete methods.
        // Verify only 'save' is called on the repository port.
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("5000.00"));
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("5000.00"));

        ledgerService.postTransaction("Append-only test", List.of(debit, credit));

        // Verify only save() is called — no delete, no update
        verify(ledgerRepositoryPort, times(1)).save(any(LedgerTransaction.class));
        verify(ledgerRepositoryPort, never()).findById(any());
        verify(ledgerRepositoryPort, never()).saveEntry(any());
        verifyNoMoreInteractions(ledgerRepositoryPort);
    }

    @Test
    public void testPostTransaction_PreciseDecimalComparison_Success() {
        // Ensure BigDecimal scale doesn't cause false imbalance (e.g., 10000 vs 10000.00)
        LedgerEntry debit = buildEntry(LedgerEntryType.DEBIT, "CASH", new BigDecimal("10000"));
        LedgerEntry credit = buildEntry(LedgerEntryType.CREDIT, "ESCROW_LIABILITY", new BigDecimal("10000.00"));

        assertDoesNotThrow(() ->
                ledgerService.postTransaction("Decimal scale test", List.of(debit, credit)));
    }

    // ========================= HELPER METHODS ========================

    private LedgerEntry buildEntry(LedgerEntryType type, String accountNumber, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setEntryType(type);
        entry.setAccountNumber(accountNumber);
        entry.setAmount(amount);
        entry.setCurrency("INR");
        return entry;
    }
}
