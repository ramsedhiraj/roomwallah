package com.roomwallah.trust.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvidence {
    private String fingerprint;
    private String context;
}
