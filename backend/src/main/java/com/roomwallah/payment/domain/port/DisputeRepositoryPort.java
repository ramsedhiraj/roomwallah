package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.PaymentDispute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepositoryPort {
    PaymentDispute save(PaymentDispute dispute);
    Optional<PaymentDispute> findById(UUID id);
    List<PaymentDispute> findByPaymentId(UUID paymentId);
    List<PaymentDispute> findAll();
    long countOpen();
}
