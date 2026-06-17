package com.roomwallah.payment.application.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, UUID> {
}
// This allows the payment bounded context to persist events to the shared outbox_events table.
