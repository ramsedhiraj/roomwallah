package com.roomwallah.payment.domain.repository;

import com.roomwallah.payment.domain.entity.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, UUID> {
    List<PaymentWebhook> findByProcessed(boolean processed);
}
