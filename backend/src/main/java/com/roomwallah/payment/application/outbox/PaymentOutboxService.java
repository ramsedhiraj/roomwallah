package com.roomwallah.payment.application.outbox;

public interface PaymentOutboxService {
    void persistEvent(String aggregateType, String aggregateId, Object domainEvent);
}
