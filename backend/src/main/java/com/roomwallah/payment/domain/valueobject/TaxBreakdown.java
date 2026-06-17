package com.roomwallah.payment.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@Embeddable
public class TaxBreakdown implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BigDecimal gstAmount;
    private final BigDecimal platformFee;
    private final BigDecimal baseAmount;
    private final BigDecimal totalAmount;
}
