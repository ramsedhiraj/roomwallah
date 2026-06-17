package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Invoice;

public interface PdfInvoiceGenerator {
    String generatePdf(Invoice invoice);
}
