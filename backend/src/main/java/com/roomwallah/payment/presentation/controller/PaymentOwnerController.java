package com.roomwallah.payment.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.payment.application.dto.EscrowAccountResponseDto;
import com.roomwallah.payment.application.dto.PayoutResponseDto;
import com.roomwallah.payment.application.facade.PaymentFacade;
import com.roomwallah.payment.presentation.dto.InitiatePayoutRequest;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Owner-facing payment endpoints — escrow accounts and payouts.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/owner/payments")
@RequiredArgsConstructor
public class PaymentOwnerController {

    private final PaymentFacade paymentFacade;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/escrow")
    public ResponseEntity<ApiResponse<List<EscrowAccountResponseDto>>> getOwnerEscrowAccounts() {
        User user = currentUserProvider.getCurrentUser();
        List<EscrowAccountResponseDto> accounts = paymentFacade.getOwnerEscrowAccounts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<PayoutResponseDto>>> getOwnerPayouts() {
        User user = currentUserProvider.getCurrentUser();
        List<PayoutResponseDto> payouts = paymentFacade.getOwnerPayouts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(payouts));
    }

    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> requestPayout(
            @Valid @RequestBody InitiatePayoutRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("Owner {} requesting payout of {}", user.getId(), request.getAmount());
        PayoutResponseDto dto = paymentFacade.initiatePayout(
                user.getId(),
                request.getAmount(),
                request.getDestinationAccount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Payout initiated"));
    }
}
