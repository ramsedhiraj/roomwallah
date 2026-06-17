package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Invoice;
import com.roomwallah.payment.domain.entity.InvoiceType;
import com.roomwallah.payment.domain.valueobject.BillingAddress;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvoiceService {
    Invoice generateInvoice(UUID bookingId, UUID paymentId, InvoiceType type, BigDecimal amount, String currency, BillingAddress billingAddress);
}
