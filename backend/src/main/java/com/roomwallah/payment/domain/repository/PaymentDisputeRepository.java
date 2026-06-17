package com.roomwallah.payment.domain.repository;

import com.roomwallah.payment.domain.entity.DisputeStatus;
import com.roomwallah.payment.domain.entity.PaymentDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, UUID> {
    List<PaymentDispute> findByPaymentId(UUID paymentId);
    long countByStatusIn(Collection<DisputeStatus> statuses);
}

