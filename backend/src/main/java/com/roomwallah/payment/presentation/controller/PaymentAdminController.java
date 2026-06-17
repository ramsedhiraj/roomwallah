package com.roomwallah.payment.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.payment.application.dto.*;
import com.roomwallah.payment.application.facade.PaymentFacade;
import com.roomwallah.payment.application.service.ReconciliationService;
import com.roomwallah.payment.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-only payment management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentFacade paymentFacade;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getAllPayments() {
        List<PaymentResponseDto> payments = paymentFacade.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> capturePayment(
            @PathVariable UUID paymentId,
            @RequestBody CapturePaymentRequest request) {
        log.info("Admin: Capturing payment {}", paymentId);
        PaymentResponseDto dto = paymentFacade.capturePayment(paymentId, request.getGatewayPaymentId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Payment captured"));
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> failPayment(
            @PathVariable UUID paymentId,
            @RequestBody FailPaymentRequest request) {
        log.info("Admin: Failing payment {}", paymentId);
        PaymentResponseDto dto = paymentFacade.failPayment(paymentId, request.getErrorReason());
        return ResponseEntity.ok(ApiResponse.success(dto, "Payment failed"));
    }

    @PostMapping("/escrow/{escrowId}/release")
    public ResponseEntity<ApiResponse<EscrowAccountResponseDto>> releaseEscrow(@PathVariable UUID escrowId) {
        log.info("Admin: Releasing escrow {}", escrowId);
        EscrowAccountResponseDto dto = paymentFacade.releaseFunds(escrowId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Escrow released"));
    }

    @PostMapping("/escrow/{escrowId}/refund")
    public ResponseEntity<ApiResponse<EscrowAccountResponseDto>> refundEscrow(@PathVariable UUID escrowId) {
        log.info("Admin: Refunding escrow {}", escrowId);
        EscrowAccountResponseDto dto = paymentFacade.refundEscrow(escrowId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Escrow refunded"));
    }

    @PostMapping("/payouts/{payoutId}/settle")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> settlePayout(
            @PathVariable UUID payoutId,
            @RequestBody SettlePayoutRequest request) {
        log.info("Admin: Settling payout {}", payoutId);
        PayoutResponseDto dto = paymentFacade.settlePayout(payoutId, request.getGatewayPayoutId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Payout settled"));
    }

    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<List<DisputeResponseDto>>> getAllDisputes() {
        List<DisputeResponseDto> disputes = paymentFacade.getAllDisputes();
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @GetMapping("/webhooks")
    public ResponseEntity<ApiResponse<List<WebhookResponseDto>>> getAllWebhooks() {
        List<WebhookResponseDto> webhooks = paymentFacade.getAllWebhooks();
        return ResponseEntity.ok(ApiResponse.success(webhooks));
    }

    @PostMapping("/webhooks/{webhookId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryWebhook(@PathVariable UUID webhookId) {
        log.info("Admin: Retrying webhook {}", webhookId);
        paymentFacade.retryWebhook(webhookId);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook retried"));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<Void>> reconcile(@RequestBody ReconcileRequest request) {
        log.info("Admin: Reconciling transactions for {}", request.getGatewayProvider());
        List<ReconciliationService.GatewayRecord> records = request.getRecords().stream()
                .map(r -> ReconciliationService.GatewayRecord.builder()
                        .gatewayPaymentId(r.getGatewayTransactionId())
                        .amount(r.getAmount())
                        .status(r.getStatus())
                        .build()
                )
                .collect(Collectors.toList());
        paymentFacade.reconcileGatewayTransactions(request.getGatewayProvider(), records);
        return ResponseEntity.ok(ApiResponse.success(null, "Reconciliation complete"));
    }
}
