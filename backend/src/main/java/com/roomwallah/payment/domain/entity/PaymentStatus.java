package com.roomwallah.payment.domain.entity;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    DISPUTED
}
