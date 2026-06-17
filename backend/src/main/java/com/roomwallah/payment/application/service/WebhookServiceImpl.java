package com.roomwallah.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.payment.domain.entity.PaymentWebhook;
import com.roomwallah.payment.domain.entity.Payout;
import com.roomwallah.payment.domain.entity.PayoutStatus;
import com.roomwallah.payment.domain.port.PayoutRepositoryPort;
import com.roomwallah.payment.domain.port.WebhookRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepositoryPort webhookRepositoryPort;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentWebhook processWebhook(String gatewayProvider, String eventType, String payloadJson) {
        log.info("Receiving webhook event: {} from provider: {}", eventType, gatewayProvider);

        PaymentWebhook webhook = new PaymentWebhook();
        webhook.setGatewayProvider(gatewayProvider);
        webhook.setEventType(eventType);
        webhook.setPayloadJson(payloadJson);
        webhook.setProcessed(false);
        PaymentWebhook savedWebhook = webhookRepositoryPort.save(webhook);

        try {
            Map<?, ?> payloadMap = objectMapper.readValue(payloadJson, Map.class);

            switch (eventType) {
                case "charge.succeeded":
                case "payment.captured":
                    handlePaymentCaptured(payloadMap);
                    break;
                case "refund.succeeded":
                case "refund.processed":
                    handleRefundSuccess(payloadMap);
                    break;
                case "payout.failed":
                    handlePayoutFailed(payloadMap);
                    break;
                default:
                    log.warn("Unhandled webhook event type: {}", eventType);
                    break;
            }

            savedWebhook.setProcessed(true);
            savedWebhook.setProcessedAt(Instant.now());
            webhookRepositoryPort.save(savedWebhook);
            log.info("Webhook ID: {} processed successfully", savedWebhook.getId());

        } catch (Exception e) {
            log.error("Failed to process webhook ID: {}", savedWebhook.getId(), e);
            savedWebhook.setProcessed(false);
            savedWebhook.setErrorReason(e.getMessage() != null ? e.getMessage() : e.toString());
            webhookRepositoryPort.save(savedWebhook);
        }

        return savedWebhook;
    }

    private void handlePaymentCaptured(Map<?, ?> payload) {
        UUID paymentId = UUID.fromString(payload.get("paymentId").toString());
        String gatewayPaymentId = payload.get("gatewayPaymentId").toString();
        paymentService.capturePayment(paymentId, gatewayPaymentId);
    }

    private void handleRefundSuccess(Map<?, ?> payload) {
        UUID refundId = UUID.fromString(payload.get("refundId").toString());
        String gatewayRefundId = payload.get("gatewayRefundId").toString();
        refundService.processRefundSuccess(refundId, gatewayRefundId);
    }

    private void handlePayoutFailed(Map<?, ?> payload) {
        UUID payoutId = UUID.fromString(payload.get("payoutId").toString());
        String reason = payload.containsKey("reason") ? payload.get("reason").toString() : "Unknown payout failure reason";
        
        Payout payout = payoutRepositoryPort.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found with ID: " + payoutId));
        
        if (payout.getStatus() != PayoutStatus.FAILED) {
            payout.setStatus(PayoutStatus.FAILED);
            payoutRepositoryPort.save(payout);
            log.warn("Payout ID: {} marked as FAILED via webhook. Reason: {}", payoutId, reason);
        }
    }
}
