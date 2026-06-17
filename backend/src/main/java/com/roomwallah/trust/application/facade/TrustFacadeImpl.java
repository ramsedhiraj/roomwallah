package com.roomwallah.trust.application.facade;

import com.roomwallah.trust.application.service.*;
import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.OwnerVerificationRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TrustFacadeImpl implements TrustFacade {

    private final VerificationSubmissionService verificationSubmissionService;
    private final VerificationApprovalService verificationApprovalService;
    private final TrustScoreService trustScoreService;
    private final BrokerDetectionService brokerDetectionService;
    private final FraudDetectionService fraudDetectionService;
    private final ModerationService moderationService;
    private final OwnerVerificationRepository ownerVerificationRepository;

    public TrustFacadeImpl(
            VerificationSubmissionService verificationSubmissionService,
            VerificationApprovalService verificationApprovalService,
            TrustScoreService trustScoreService,
            BrokerDetectionService brokerDetectionService,
            FraudDetectionService fraudDetectionService,
            ModerationService moderationService,
            OwnerVerificationRepository ownerVerificationRepository) {
        this.verificationSubmissionService = verificationSubmissionService;
        this.verificationApprovalService = verificationApprovalService;
        this.trustScoreService = trustScoreService;
        this.brokerDetectionService = brokerDetectionService;
        this.fraudDetectionService = fraudDetectionService;
        this.moderationService = moderationService;
        this.ownerVerificationRepository = ownerVerificationRepository;
    }

    @Override
    public OwnerVerification submitVerification(UUID userId, VerificationLevel level, VerificationProvider provider, List<UUID> mediaIds, String idempotencyKey) {
        return verificationSubmissionService.submitVerification(userId, level, provider, mediaIds, idempotencyKey);
    }

    @Override
    public OwnerVerification approveVerification(UUID verificationId, UUID reviewerId) {
        return verificationApprovalService.approveVerification(verificationId, reviewerId);
    }

    @Override
    public OwnerVerification rejectVerification(UUID verificationId, UUID reviewerId, String reason) {
        return verificationApprovalService.rejectVerification(verificationId, reviewerId, reason);
    }

    @Override
    public TrustScore recalculateTrustScore(UUID userId, String triggerEvent) {
        return trustScoreService.recalculateTrustScore(userId, triggerEvent);
    }

    @Override
    public Optional<TrustScore> getTrustScore(UUID userId) {
        return trustScoreService.getTrustScore(userId);
    }

    @Override
    public Optional<OwnerVerification> getOwnerVerification(UUID userId) {
        return ownerVerificationRepository.findByUserId(userId);
    }

    @Override
    public List<BrokerDetectionSignal> runBrokerChecks(UUID userId) {
        return brokerDetectionService.runDetection(userId);
    }

    @Override
    public List<FraudSignal> runFraudChecks(UUID userId) {
        return fraudDetectionService.runFraudChecks(userId);
    }

    @Override
    public void analyzeNetworkRisk(UUID userId, String ipAddress, String userAgent) {
        fraudDetectionService.analyzeNetworkRisk(userId, ipAddress, userAgent);
    }

    @Override
    public List<ModerationCase> getOpenCases() {
        return moderationService.getOpenCases();
    }

    @Override
    public ModerationCase assignAdmin(UUID caseId, UUID adminId) {
        return moderationService.assignAdmin(caseId, adminId);
    }

    @Override
    public ModerationCase resolveCase(UUID caseId, String notes) {
        return moderationService.resolveCase(caseId, notes);
    }
}
