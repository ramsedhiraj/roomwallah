package com.roomwallah.payment.application.adapter;

import com.roomwallah.payment.domain.entity.Invoice;
import com.roomwallah.payment.domain.port.InvoiceRepositoryPort;
import com.roomwallah.payment.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final InvoiceRepository invoiceRepository;

    @Override
    public Invoice save(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    public Optional<Invoice> findByBookingId(UUID bookingId) {
        return invoiceRepository.findByBookingId(bookingId);
    }
}
