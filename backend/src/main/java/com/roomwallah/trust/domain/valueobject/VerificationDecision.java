package com.roomwallah.trust.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDecision {
    private String reason;
    private String previousStatus;
    private String newStatus;
}
