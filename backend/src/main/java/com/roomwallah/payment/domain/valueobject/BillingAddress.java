package com.roomwallah.payment.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@Embeddable
public class BillingAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String street;
    private final String city;
    private final String state;
    private final String country;
    private final String zipCode;
}
