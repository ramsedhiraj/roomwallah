package com.roomwallah.payment.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.payment.application.facade.PaymentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated external webhook receiver for payment gateways.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/{provider}")
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-Event-Type", required = false) String eventTypeHeader,
            @RequestBody String payloadJson) {
        String eventType = eventTypeHeader != null ? eventTypeHeader : "unknown";
        log.info("Webhook received from provider: {}, eventType: {}", provider, eventType);
        paymentFacade.processWebhook(provider, eventType, payloadJson);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed"));
    }
}
