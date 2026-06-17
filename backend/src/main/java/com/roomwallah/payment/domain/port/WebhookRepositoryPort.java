package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.PaymentWebhook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookRepositoryPort {
    PaymentWebhook save(PaymentWebhook webhook);
    Optional<PaymentWebhook> findById(UUID id);
    List<PaymentWebhook> findByProcessed(boolean processed);
    List<PaymentWebhook> findAll();
}
