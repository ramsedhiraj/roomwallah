package com.roomwallah.payment.application.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.payment.domain.entity.Payment;
import com.roomwallah.payment.domain.entity.PaymentStatus;
import com.roomwallah.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;

    @Override
    @Transactional
    public void reconcileGatewayTransactions(String gatewayProvider, List<GatewayRecord> gatewayRecords) {
        log.info("Starting reconciliation sweep for gateway provider: {} with {} records", gatewayProvider, gatewayRecords.size());

        int processedCount = 0;
        int discrepancyCount = 0;
        int missingCount = 0;

        for (GatewayRecord record : gatewayRecords) {
            Optional<Payment> optionalPayment = paymentRepository.findAll().stream()
                    .filter(p -> record.getGatewayPaymentId() != null && record.getGatewayPaymentId().equals(p.getGatewayPaymentId()))
                    .findFirst();

            if (optionalPayment.isEmpty()) {
                log.error("Reconciliation Mismatch: Local payment record not found for gatewayPaymentId: {}", record.getGatewayPaymentId());
                missingCount++;
                
                Map<String, Object> details = new HashMap<>();
                details.put("gatewayPaymentId", record.getGatewayPaymentId());
                details.put("amount", record.getAmount());
                details.put("status", record.getStatus());
                auditPort.log("RECONCILIATION_MISSING_LOCAL_PAYMENT", "SYSTEM", "SYSTEM", details);
                continue;
            }

            Payment payment = optionalPayment.get();
            boolean isAmountMatch = payment.getAmount().compareTo(record.getAmount()) == 0;
            boolean isStatusMatch = mapGatewayStatus(record.getStatus()) == payment.getStatus();

            if (!isAmountMatch || !isStatusMatch) {
                log.error("Reconciliation Discrepancy: payment ID: {} (gateway ID: {}). Local[amount={}, status={}] vs Gateway[amount={}, status={}]",
                        payment.getId(), record.getGatewayPaymentId(), payment.getAmount(), payment.getStatus(), record.getAmount(), record.getStatus());
                discrepancyCount++;

                Map<String, Object> details = new HashMap<>();
                details.put("paymentId", payment.getId().toString());
                details.put("gatewayPaymentId", record.getGatewayPaymentId());
                details.put("localAmount", payment.getAmount());
                details.put("gatewayAmount", record.getAmount());
                details.put("localStatus", payment.getStatus().name());
                details.put("gatewayStatus", record.getStatus());
                auditPort.log("RECONCILIATION_DISCREPANCY", "SYSTEM", "SYSTEM", details);

                // Notify Admin
                notificationPort.sendEmail("admin@roomwallah.com", 
                        "Reconciliation Alert: Payment Discrepancy", 
                        "A discrepancy was found during reconciliation sweep for Payment ID: " + payment.getId()
                        + "\nLocal state: " + payment.getStatus() + " (" + payment.getAmount() + " INR)"
                        + "\nGateway state: " + record.getStatus() + " (" + record.getAmount() + " INR)");
            } else {
                processedCount++;
            }
        }

        log.info("Reconciliation sweep completed. Processed/Matched: {}, Discrepancies: {}, Missing locally: {}", 
                processedCount, discrepancyCount, missingCount);
    }

    private PaymentStatus mapGatewayStatus(String gatewayStatus) {
        if (gatewayStatus == null) return null;
        switch (gatewayStatus.toUpperCase()) {
            case "SUCCEEDED":
            case "CAPTURED":
            case "SUCCESS":
                return PaymentStatus.CAPTURED;
            case "FAILED":
            case "FAIL":
                return PaymentStatus.FAILED;
            case "REFUNDED":
            case "REFUND":
                return PaymentStatus.REFUNDED;
            default:
                return PaymentStatus.PENDING;
        }
    }
}
