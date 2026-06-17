package com.roomwallah.payment.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import com.roomwallah.payment.domain.valueobject.BillingAddress;
import com.roomwallah.payment.domain.valueobject.BillingAddressConverter;
import com.roomwallah.payment.domain.valueobject.TaxBreakdown;
import com.roomwallah.payment.domain.valueobject.TaxBreakdownConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @Column(name = "booking_id", nullable = false, columnDefinition = "UUID")
    private UUID bookingId;

    @Column(name = "payment_id", columnDefinition = "UUID")
    private UUID paymentId;

    @Column(name = "refund_id", columnDefinition = "UUID")
    private UUID refundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private InvoiceType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "billing_address", columnDefinition = "TEXT")
    @Convert(converter = BillingAddressConverter.class)
    private BillingAddress billingAddress;

    @Column(name = "tax_breakdown_json", columnDefinition = "TEXT")
    @Convert(converter = TaxBreakdownConverter.class)
    private TaxBreakdown taxBreakdown;

    @Column(name = "pdf_path", length = 512)
    private String pdfPath;
}
