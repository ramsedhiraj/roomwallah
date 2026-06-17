package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Invoice;
import com.roomwallah.payment.domain.entity.InvoiceType;
import com.roomwallah.payment.domain.port.InvoiceRepositoryPort;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import com.roomwallah.payment.domain.valueobject.TaxBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;

    @Override
    @Transactional
    public Invoice generateInvoice(UUID bookingId, UUID paymentId, InvoiceType type, BigDecimal amount, String currency, BillingAddress billingAddress) {
        log.info("Generating invoice for booking: {}, payment: {}, type: {}, amount: {}", bookingId, paymentId, type, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invoice amount must be positive");
        }

        // Calculate tax breakdown logically: platform fee is 5%, base is 95%, gst is 18% of base
        BigDecimal platformFee = amount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseAmount = amount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = baseAmount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);

        TaxBreakdown taxBreakdown = TaxBreakdown.builder()
                .baseAmount(baseAmount)
                .platformFee(platformFee)
                .gstAmount(gstAmount)
                .totalAmount(amount)
                .build();

        String invoiceNumber = "INV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setBookingId(bookingId);
        invoice.setPaymentId(paymentId);
        invoice.setType(type);
        invoice.setAmount(amount);
        invoice.setCurrency(currency != null ? currency : "INR");
        invoice.setBillingAddress(billingAddress);
        invoice.setTaxBreakdown(taxBreakdown);

        // Generate PDF and save path
        String pdfPath = pdfInvoiceGenerator.generatePdf(invoice);
        invoice.setPdfPath(pdfPath);

        Invoice saved = invoiceRepositoryPort.save(invoice);
        log.info("Invoice generated and saved successfully with ID: {}, Number: {}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }
}
