package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.PaymentWebhook;

public interface WebhookService {
    PaymentWebhook processWebhook(String gatewayProvider, String eventType, String payloadJson);
}
