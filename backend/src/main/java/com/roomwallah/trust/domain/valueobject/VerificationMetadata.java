package com.roomwallah.trust.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationMetadata {
    private Map<String, Object> rawDetails;
}
