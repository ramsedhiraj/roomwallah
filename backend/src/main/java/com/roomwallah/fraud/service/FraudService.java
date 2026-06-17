package com.roomwallah.fraud.service;

import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.fraud.domain.FraudCase;
import com.roomwallah.fraud.domain.FraudEvent;
import com.roomwallah.fraud.domain.FraudRuleSet;
import com.roomwallah.fraud.repository.FraudCaseRepository;
import com.roomwallah.fraud.repository.FraudEventRepository;
import com.roomwallah.fraud.repository.FraudRuleSetRepository;
import com.roomwallah.payment.domain.entity.PaymentStatus;
import com.roomwallah.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudEventRepository fraudEventRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final FraudRuleSetRepository fraudRuleSetRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public FraudCase evaluateUserRisk(UUID userId, String ipAddress, String deviceToken) {
        log.info("Running fraud risk heuristics for user: {}", userId);

        // Retrieve active rule set
        FraudRuleSet ruleSet = fraudRuleSetRepository.findAll().stream()
                .filter(FraudRuleSet::isActive)
                .findFirst()
                .orElseGet(() -> {
                    FraudRuleSet defaultSet = FraudRuleSet.builder()
                            .versionName("v1.default")
                            .velocityLimit(3)
                            .largeTransactionLimit(BigDecimal.valueOf(100000.00))
                            .active(true)
                            .build();
                    defaultSet.setVersion(0L);
                    return fraudRuleSetRepository.save(defaultSet);
                });

        double riskScore = 0.0;
        List<String> reasons = new ArrayList<>();

        // 1. IP / location mismatch heuristic
        if (ipAddress != null && !ipAddress.isBlank()) {
            if (!ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1") && !ipAddress.startsWith("192.168.")) {
                riskScore += 25.0;
                reasons.add("IP anomaly detected: Request origin mismatch");
            }
        }

        // 2. Velocity limit using versioned ruleset limit
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getTenantId().equals(userId))
                .filter(b -> b.getCreatedAt().isAfter(oneHourAgo))
                .count();
        if (recentBookings > ruleSet.getVelocityLimit()) {
            riskScore += 35.0;
            reasons.add("Velocity check exceeded: " + recentBookings + " booking requests in the last hour (Limit: " + ruleSet.getVelocityLimit() + ")");
        }

        // 3. Failed payment checks
        long failedPayments = paymentRepository.findByTenantId(userId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                .count();
        if (failedPayments > 2) {
            riskScore += 20.0;
            reasons.add("Payment checks failed: Multiple payment attempts failed (" + failedPayments + ")");
        }

        // 4. Large transactions using versioned ruleset limit
        boolean hasLargeTransactions = paymentRepository.findByTenantId(userId).stream()
                .anyMatch(p -> p.getAmount().compareTo(ruleSet.getLargeTransactionLimit()) > 0);
        if (hasLargeTransactions) {
            riskScore += 20.0;
            reasons.add("Payout velocity check: Large single transaction volume flagged (> " + ruleSet.getLargeTransactionLimit() + " INR)");
        }

        riskScore = Math.min(riskScore, 100.0);

        if (riskScore > 0.0) {
            FraudEvent event = FraudEvent.builder()
                    .userId(userId)
                    .eventType("HEURISTICS_CHECK")
                    .details(String.join("; ", reasons))
                    .riskScore(BigDecimal.valueOf(riskScore))
                    .status("PROCESSED")
                    .build();
            fraudEventRepository.save(event);
        }

        if (riskScore >= 40.0) {
            FraudCase fraudCase = fraudCaseRepository.findByUserId(userId).stream()
                    .filter(c -> "PENDING_REVIEW".equals(c.getStatus()) || "ESCALATED".equals(c.getStatus()))
                    .findFirst()
                    .orElseGet(() -> {
                        FraudCase newCase = FraudCase.builder()
                                .userId(userId)
                                .status("PENDING_REVIEW")
                                .build();
                        newCase.setVersion(0L);
                        return newCase;
                    });

            fraudCase.setRiskScore(BigDecimal.valueOf(riskScore));
            fraudCase.setReason(String.join("; ", reasons));
            fraudCase.setRuleSetVersion(ruleSet.getVersionName());
            return fraudCaseRepository.save(fraudCase);
        }

        return null;
    }

    @Transactional(readOnly = true)
    public List<FraudCase> getCases(String status) {
        if (status != null && !status.isBlank()) {
            return fraudCaseRepository.findByStatus(status);
        }
        return fraudCaseRepository.findAll();
    }

    @Transactional
    public FraudCase resolveCase(UUID caseId, UUID reviewerId, String status, String notes) {
        FraudCase fc = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + caseId));
        
        fc.setStatus(status);
        fc.setReviewerId(reviewerId);
        fc.setReviewerNotes(notes);
        fc.setResolvedAt(Instant.now());
        return fraudCaseRepository.save(fc);
    }

    @Transactional
    public FraudCase escalateCase(UUID caseId, String escalatedTo) {
        FraudCase fc = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud case not found: " + caseId));
        
        fc.setStatus("ESCALATED");
        fc.setEscalatedTo(escalatedTo);
        fc.setEscalatedAt(Instant.now());
        return fraudCaseRepository.save(fc);
    }

    @Transactional
    public FraudRuleSet createOrUpdateRuleSet(String versionName, int velocityLimit, BigDecimal largeTransactionLimit, boolean active) {
        if (active) {
            // Deactivate other rule sets
            fraudRuleSetRepository.findAll().forEach(rs -> {
                if (rs.isActive()) {
                    rs.setActive(false);
                    fraudRuleSetRepository.save(rs);
                }
            });
        }

        FraudRuleSet ruleSet = fraudRuleSetRepository.findByVersionName(versionName)
                .orElseGet(() -> FraudRuleSet.builder().versionName(versionName).build());

        ruleSet.setVelocityLimit(velocityLimit);
        ruleSet.setLargeTransactionLimit(largeTransactionLimit);
        ruleSet.setActive(active);
        ruleSet.setVersion(0L);
        return fraudRuleSetRepository.save(ruleSet);
    }
}
