package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.PaymentWebhook;
import com.roomwallah.payment.domain.port.WebhookRepositoryPort;
import com.roomwallah.payment.domain.repository.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebhookRepositoryAdapter implements WebhookRepositoryPort {

    private final PaymentWebhookRepository webhookRepository;

    @Override
    public PaymentWebhook save(PaymentWebhook webhook) {
        return webhookRepository.save(webhook);
    }

    @Override
    public Optional<PaymentWebhook> findById(UUID id) {
        return webhookRepository.findById(id);
    }

    @Override
    public List<PaymentWebhook> findByProcessed(boolean processed) {
        return webhookRepository.findByProcessed(processed);
    }

    @Override
    public List<PaymentWebhook> findAll() {
        return webhookRepository.findAll();
    }
}

