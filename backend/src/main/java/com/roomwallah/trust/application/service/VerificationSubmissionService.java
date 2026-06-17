package com.roomwallah.trust.application.service;

import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.*;
import com.roomwallah.trust.domain.event.VerificationSubmittedEvent;
import com.roomwallah.trust.infrastructure.adapter.TrustEncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class VerificationSubmissionService {

    private final OwnerVerificationRepository ownerVerificationRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final OCRPort ocrPort;
    private final FaceMatchPort faceMatchPort;
    private final IdentityVerificationPort identityVerificationPort;
    private final TrustEncryptionService encryptionService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TrustScoreService trustScoreService;
    private final ModerationService moderationService;

    public VerificationSubmissionService(
            OwnerVerificationRepository ownerVerificationRepository,
            VerificationDocumentRepository verificationDocumentRepository,
            OCRPort ocrPort,
            FaceMatchPort faceMatchPort,
            IdentityVerificationPort identityVerificationPort,
            TrustEncryptionService encryptionService,
            OutboxEventPublisher outboxEventPublisher,
            TrustScoreService trustScoreService,
            ModerationService moderationService) {
        this.ownerVerificationRepository = ownerVerificationRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.ocrPort = ocrPort;
        this.faceMatchPort = faceMatchPort;
        this.identityVerificationPort = identityVerificationPort;
        this.encryptionService = encryptionService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.trustScoreService = trustScoreService;
        this.moderationService = moderationService;
    }

    @Transactional
    public OwnerVerification submitVerification(UUID userId, VerificationLevel level, VerificationProvider provider, List<UUID> mediaIds, String idempotencyKey) {
        log.info("Submitting verification for user: {}, level: {}, provider: {}", userId, level, provider);

        if (idempotencyKey != null) {
            var existing = ownerVerificationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate request detected with idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // Deactivate old verifications if any
        ownerVerificationRepository.findByUserId(userId).ifPresent(old -> {
            if (old.getVerificationStatus() == VerificationStatus.PENDING) {
                old.setVerificationStatus(VerificationStatus.EXPIRED);
                old.setExpiresAt(Instant.now());
                ownerVerificationRepository.save(old);
            }
        });

        OwnerVerification verification = OwnerVerification.builder()
                .userId(userId)
                .verificationStatus(VerificationStatus.PENDING)
                .verificationLevel(level)
                .verificationProvider(provider)
                .submittedAt(Instant.now())
                .idempotencyKey(idempotencyKey)
                .build();

        verification = ownerVerificationRepository.save(verification);

        // Save documents
        if (mediaIds != null && !mediaIds.isEmpty()) {
            for (UUID mediaId : mediaIds) {
                // OCR analysis mock call
                String ocrText = ocrPort.extractText(mediaId);
                String encryptedOcr = encryptionService.encrypt(ocrText);

                VerificationDocument doc = VerificationDocument.builder()
                        .verificationId(verification.getId())
                        .mediaId(mediaId)
                        .documentType("ID_DOCUMENT")
                        .encryptedMetadata(encryptedOcr)
                        .uploadedAt(Instant.now())
                        .build();

                verificationDocumentRepository.save(doc);
            }
        }

        // Run face match check if there are at least two documents (e.g. ID and selfie)
        boolean faceMatchFailed = false;
        if (mediaIds != null && mediaIds.size() >= 2) {
            double matchScore = faceMatchPort.matchFaces(mediaIds.get(0), mediaIds.get(1));
            if (matchScore < 0.85) {
                log.warn("Face match score too low: {} for user: {}", matchScore, userId);
                faceMatchFailed = true;
            }
        }

        // Verify with identity verification provider
        var meta = identityVerificationPort.verifyIdentity(userId, verification);
        boolean externalCheckPassed = meta.getRawDetails() != null && "SUCCESS".equals(meta.getRawDetails().get("status"));

        if (faceMatchFailed || !externalCheckPassed) {
            // Flag for admin review by creating a moderation case
            moderationService.createCase("OWNER_VERIFICATION", verification.getId(), new java.math.BigDecimal("8.5000"));
            log.info("Verification flagged for manual review due to face match or external check failure.");
        } else {
            // Auto approve
            verification.setVerificationStatus(VerificationStatus.APPROVED);
            verification.setApprovedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(365)));
            ownerVerificationRepository.save(verification);
            log.info("Verification auto-approved for user: {}", userId);
        }

        outboxEventPublisher.persistEvent("OwnerVerification", verification.getId().toString(),
                new VerificationSubmittedEvent(verification.getId(), userId, Instant.now()));

        // Recalculate trust score
        trustScoreService.recalculateTrustScore(userId, "VERIFICATION_SUBMISSION");

        return verification;
    }
}
