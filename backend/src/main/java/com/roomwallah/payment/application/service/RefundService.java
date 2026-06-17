package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Refund;
import java.math.BigDecimal;
import java.util.UUID;

public interface RefundService {
    Refund initiateRefund(UUID paymentId, BigDecimal amount, String reason);
    Refund processRefundSuccess(UUID refundId, String gatewayRefundId);
}
