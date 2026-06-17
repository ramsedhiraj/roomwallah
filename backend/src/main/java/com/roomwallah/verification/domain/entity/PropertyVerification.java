package com.roomwallah.verification.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "property_verifications")
@Getter
@Setter
public class PropertyVerification extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "document_url", nullable = false, length = 512)
    private String documentUrl;

    @Column(name = "utility_bill_url", nullable = false, length = 512)
    private String utilityBillUrl;

    @Column(name = "deed_name_matched", nullable = false)
    private boolean deedNameMatched = false;

    @Column(name = "utility_name_matched", nullable = false)
    private boolean utilityNameMatched = false;

    @Column(name = "location_matched", nullable = false)
    private boolean locationMatched = false;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
