package com.roomwallah.trust.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustExplanation {
    private List<String> reasons;
    private Map<String, Object> factors;
}
