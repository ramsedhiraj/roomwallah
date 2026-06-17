package com.roomwallah.verification.application.facade;

import com.roomwallah.verification.application.service.*;
import com.roomwallah.verification.domain.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VerificationFacadeImpl implements VerificationFacade {

    private final VerificationSubmissionService submissionService;
    private final VerificationReviewService reviewService;
    private final TrustScoreService trustScoreService;
    private final BrokerDetectionService brokerDetectionService;
    private final VerificationHistoryService historyService;

    @Override
    public VerificationRequest submit(UUID userId, VerificationProvider provider, String code, String idempotencyKey) {
        return submissionService.submit(userId, provider, code, idempotencyKey);
    }

    @Override
    public VerificationRequest getActiveVerification(UUID userId) {
        return submissionService.getActiveVerification(userId);
    }

    @Override
    public TrustScore getTrustScore(UUID userId) {
        return trustScoreService.getTrustScore(userId);
    }

    @Override
    public List<FraudSignal> getFraudSignals(UUID userId) {
        return brokerDetectionService.getFraudSignals(userId);
    }

    @Override
    public List<FraudSignal> getAllFraudSignals() {
        return brokerDetectionService.getAllFraudSignals();
    }

    @Override
    public List<VerificationRequest> getPendingRequests() {
        return reviewService.getPendingRequests();
    }

    @Override
    public VerificationRequest approve(UUID requestId, UUID adminId, String reason) {
        return reviewService.approve(requestId, adminId, reason);
    }

    @Override
    public VerificationRequest reject(UUID requestId, UUID adminId, String reason) {
        return reviewService.reject(requestId, adminId, reason);
    }

    @Override
    public VerificationRequest escalate(UUID requestId, UUID adminId, String reason) {
        return reviewService.escalate(requestId, adminId, reason);
    }

    @Override
    public VerificationRequest reopen(UUID requestId, UUID adminId, String reason) {
        return reviewService.reopen(requestId, adminId, reason);
    }

    @Override
    public VerificationRequest revoke(UUID requestId, UUID adminId, String reason) {
        return reviewService.revoke(requestId, adminId, reason);
    }

    @Override
    public VerificationRequest expire(UUID requestId, UUID adminId, String reason) {
        return reviewService.expire(requestId, adminId, reason);
    }

    @Override
    public List<VerificationDecisionAudit> getDecisionHistory(UUID requestId) {
        return historyService.getHistory(requestId);
    }
}
