package com.roomwallah.payment.application.service;

import com.roomwallah.payment.domain.entity.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
@Component
public class PdfInvoiceGeneratorImpl implements PdfInvoiceGenerator {

    @Override
    public String generatePdf(Invoice invoice) {
        log.info("Generating PDF for invoice: {}", invoice.getInvoiceNumber());
        try {
            File tempDir = new File(System.getProperty("user.home") + "/.gemini/antigravity/scratch/roomwallah_invoices");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            File invoiceFile = new File(tempDir, "invoice_" + invoice.getInvoiceNumber() + ".pdf");
            try (java.io.Writer writer = java.nio.file.Files.newBufferedWriter(invoiceFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write("%PDF-1.4 Mock PDF Invoice\n");
                writer.write("Invoice Number: " + invoice.getInvoiceNumber() + "\n");
                writer.write("Type: " + invoice.getType() + "\n");
                writer.write("Amount: " + invoice.getAmount() + " " + invoice.getCurrency() + "\n");
                writer.write("Booking ID: " + invoice.getBookingId() + "\n");
            }
            log.info("Mock PDF Invoice generated at: {}", invoiceFile.getAbsolutePath());
            return invoiceFile.getAbsolutePath();
        } catch (IOException e) {
            log.error("Failed to generate PDF for invoice", e);
            throw new RuntimeException("PDF generation error", e);
        }
    }
}
