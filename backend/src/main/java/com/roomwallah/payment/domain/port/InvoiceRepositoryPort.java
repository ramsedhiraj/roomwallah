package com.roomwallah.payment.domain.port;

import com.roomwallah.payment.domain.entity.Invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByBookingId(UUID bookingId);
}
