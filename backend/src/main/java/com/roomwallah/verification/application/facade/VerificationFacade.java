package com.roomwallah.verification.application.facade;

import com.roomwallah.verification.domain.entity.*;
import java.util.List;
import java.util.UUID;

public interface VerificationFacade {
    VerificationRequest submit(UUID userId, VerificationProvider provider, String code, String idempotencyKey);
    VerificationRequest getActiveVerification(UUID userId);
    TrustScore getTrustScore(UUID userId);
    List<FraudSignal> getFraudSignals(UUID userId);
    List<FraudSignal> getAllFraudSignals();
    List<VerificationRequest> getPendingRequests();
    VerificationRequest approve(UUID requestId, UUID adminId, String reason);
    VerificationRequest reject(UUID requestId, UUID adminId, String reason);
    VerificationRequest escalate(UUID requestId, UUID adminId, String reason);
    VerificationRequest reopen(UUID requestId, UUID adminId, String reason);
    VerificationRequest revoke(UUID requestId, UUID adminId, String reason);
    VerificationRequest expire(UUID requestId, UUID adminId, String reason);
    List<VerificationDecisionAudit> getDecisionHistory(UUID requestId);
}
