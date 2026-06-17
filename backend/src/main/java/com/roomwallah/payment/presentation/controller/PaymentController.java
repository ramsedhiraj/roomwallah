package com.roomwallah.payment.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.payment.application.dto.*;
import com.roomwallah.payment.application.facade.PaymentFacade;
import com.roomwallah.payment.presentation.dto.InitiatePaymentRequest;
import com.roomwallah.payment.presentation.dto.InitiateRefundRequest;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tenant-facing payment endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("User {} initiating payment for booking {}", user.getId(), request.getBookingId());
        PaymentResponseDto dto = paymentFacade.initiatePayment(
                request.getBookingId(),
                user.getId(),
                request.getOwnerId(),
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "INR",
                request.getGatewayProvider(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Payment initiated"));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPayment(@PathVariable UUID paymentId) {
        PaymentResponseDto dto = paymentFacade.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getMyPayments() {
        User user = currentUserProvider.getCurrentUser();
        List<PaymentResponseDto> payments = paymentFacade.getMyPayments(user.getId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<ApiResponse<RefundResponseDto>> initiateRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody InitiateRefundRequest request) {
        log.info("Initiating refund for payment {}", paymentId);
        RefundResponseDto dto = paymentFacade.initiateRefund(paymentId, request.getAmount(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Refund initiated"));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getInvoice(@PathVariable UUID invoiceId) {
        InvoiceResponseDto dto = paymentFacade.getInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/invoices/my")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getMyInvoices() {
        User user = currentUserProvider.getCurrentUser();
        List<PaymentResponseDto> payments = paymentFacade.getMyPayments(user.getId());
        List<InvoiceResponseDto> invoices = payments.stream()
                .map(p -> {
                    try {
                        return paymentFacade.getInvoiceByBookingId(p.getBookingId());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }
}
