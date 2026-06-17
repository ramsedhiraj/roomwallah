package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.TrustScore;
import java.util.UUID;

public interface TrustScoreCalculatorPort {
    TrustScore calculate(UUID userId);
}
