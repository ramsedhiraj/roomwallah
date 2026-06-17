package com.roomwallah.fraud;

import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.fraud.domain.FraudCase;
import com.roomwallah.fraud.domain.FraudEvent;
import com.roomwallah.fraud.domain.FraudRuleSet;
import com.roomwallah.fraud.repository.FraudCaseRepository;
import com.roomwallah.fraud.repository.FraudEventRepository;
import com.roomwallah.fraud.repository.FraudRuleSetRepository;
import com.roomwallah.fraud.service.FraudService;
import com.roomwallah.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FraudServiceTest {

    @Mock
    private FraudEventRepository fraudEventRepository;

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private FraudRuleSetRepository fraudRuleSetRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private FraudService fraudService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        fraudService = new FraudService(
                fraudEventRepository, fraudCaseRepository, fraudRuleSetRepository,
                bookingRepository, paymentRepository
        );
    }

    @Test
    public void testEvaluateUserRisk_AnomaliesFlagged() {
        UUID userId = UUID.randomUUID();

        // Mock active ruleset
        FraudRuleSet rs = FraudRuleSet.builder()
                .versionName("v1.test")
                .velocityLimit(3)
                .largeTransactionLimit(BigDecimal.valueOf(50000.00))
                .active(true)
                .build();
        when(fraudRuleSetRepository.findAll()).thenReturn(List.of(rs));

        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        when(paymentRepository.findByTenantId(userId)).thenReturn(Collections.emptyList());
        when(fraudCaseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Perform risk evaluation with an IP mismatch
        FraudCase result = fraudService.evaluateUserRisk(userId, "198.51.100.1", "device123");

        // Risk score should trigger event log
        verify(fraudEventRepository, times(1)).save(any(FraudEvent.class));
    }

    @Test
    public void testResolveCase() {
        UUID caseId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        FraudCase fc = new FraudCase();
        fc.setId(caseId);

        when(fraudCaseRepository.findById(caseId)).thenReturn(java.util.Optional.of(fc));
        when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(i -> i.getArgument(0));

        FraudCase resolved = fraudService.resolveCase(caseId, reviewerId, "RESOLVED_SAFE", "Looks benign.");

        assertEquals("RESOLVED_SAFE", resolved.getStatus());
        assertEquals(reviewerId, resolved.getReviewerId());
        assertEquals("Looks benign.", resolved.getReviewerNotes());
        assertNotNull(resolved.getResolvedAt());
    }
}
