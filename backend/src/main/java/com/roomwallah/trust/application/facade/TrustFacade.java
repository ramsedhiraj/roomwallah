package com.roomwallah.trust.application.facade;

import com.roomwallah.trust.domain.entity.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustFacade {
    OwnerVerification submitVerification(UUID userId, VerificationLevel level, VerificationProvider provider, List<UUID> mediaIds, String idempotencyKey);
    OwnerVerification approveVerification(UUID verificationId, UUID reviewerId);
    OwnerVerification rejectVerification(UUID verificationId, UUID reviewerId, String reason);
    TrustScore recalculateTrustScore(UUID userId, String triggerEvent);
    Optional<TrustScore> getTrustScore(UUID userId);
    Optional<OwnerVerification> getOwnerVerification(UUID userId);
    List<BrokerDetectionSignal> runBrokerChecks(UUID userId);
    List<FraudSignal> runFraudChecks(UUID userId);
    void analyzeNetworkRisk(UUID userId, String ipAddress, String userAgent);
    List<ModerationCase> getOpenCases();
    ModerationCase assignAdmin(UUID caseId, UUID adminId);
    ModerationCase resolveCase(UUID caseId, String notes);
}
