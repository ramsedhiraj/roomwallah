package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.TrustScore;
import java.util.UUID;

public interface TrustScoreService {
    TrustScore calculateAndSave(UUID userId);
    TrustScore getTrustScore(UUID userId);
}
