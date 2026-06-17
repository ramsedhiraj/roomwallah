package com.roomwallah.trust;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.roomwallah.trust.application.service.ModerationService;
import com.roomwallah.trust.application.service.OutboxEventPublisher;
import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.application.service.VerificationSubmissionService;
import com.roomwallah.trust.domain.entity.*;
import com.roomwallah.trust.domain.port.*;
import com.roomwallah.trust.domain.valueobject.VerificationMetadata;
import com.roomwallah.trust.infrastructure.adapter.TrustEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VerificationWorkflowTest {

    @Mock
    private OwnerVerificationRepository ownerVerificationRepository;

    @Mock
    private VerificationDocumentRepository verificationDocumentRepository;

    @Mock
    private OCRPort ocrPort;

    @Mock
    private FaceMatchPort faceMatchPort;

    @Mock
    private IdentityVerificationPort identityVerificationPort;

    @Mock
    private TrustEncryptionService encryptionService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private ModerationService moderationService;

    private VerificationSubmissionService submissionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        submissionService = new VerificationSubmissionService(
                ownerVerificationRepository,
                verificationDocumentRepository,
                ocrPort,
                faceMatchPort,
                identityVerificationPort,
                encryptionService,
                outboxEventPublisher,
                trustScoreService,
                moderationService
        );
    }

    @Test
    public void testSubmitVerification_NewSubmission_AutoApproved_WhenFaceAndExtMatchPass() {
        UUID userId = UUID.randomUUID();
        UUID docMediaId = UUID.randomUUID();
        UUID selfieMediaId = UUID.randomUUID();
        List<UUID> mediaIds = List.of(docMediaId, selfieMediaId);

        // Mocks setup
        when(ownerVerificationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(ownerVerificationRepository.save(any(OwnerVerification.class))).thenAnswer(invocation -> {
            OwnerVerification ov = invocation.getArgument(0);
            if (ov.getId() == null) ov.setId(UUID.randomUUID());
            return ov;
        });

        when(ocrPort.extractText(any(UUID.class))).thenReturn("SAMPLE OCR TEXT");
        when(encryptionService.encrypt(anyString())).thenReturn("ENCRYPTED_TEXT");
        
        // High face match score (>= 0.85)
        when(faceMatchPort.matchFaces(any(UUID.class), any(UUID.class))).thenReturn(0.92);

        // Valid identity provider response
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("status", "SUCCESS");
        VerificationMetadata metadata = new VerificationMetadata(details);
        when(identityVerificationPort.verifyIdentity(eq(userId), any())).thenReturn(metadata);

        // Execute
        OwnerVerification result = submissionService.submitVerification(
                userId, 
                VerificationLevel.STANDARD, 
                VerificationProvider.JUMIO, 
                mediaIds, 
                "key_123"
        );

        // Asserts
        assertNotNull(result);
        assertEquals(VerificationStatus.APPROVED, result.getVerificationStatus());
        assertNotNull(result.getApprovedAt());
        assertNotNull(result.getExpiresAt());

        // Verify documents saved
        verify(verificationDocumentRepository, times(2)).save(any(VerificationDocument.class));

        // Verify no moderation case created since it is auto-approved
        verify(moderationService, never()).createCase(anyString(), any(UUID.class), any());

        // Verify outbox publishes
        verify(outboxEventPublisher).persistEvent(eq("OwnerVerification"), anyString(), any());
        verify(trustScoreService).recalculateTrustScore(eq(userId), eq("VERIFICATION_SUBMISSION"));
    }

    @Test
    public void testSubmitVerification_FlagsModerationCase_WhenFaceMatchFails() {
        UUID userId = UUID.randomUUID();
        UUID docMediaId = UUID.randomUUID();
        UUID selfieMediaId = UUID.randomUUID();
        List<UUID> mediaIds = List.of(docMediaId, selfieMediaId);

        when(ownerVerificationRepository.save(any(OwnerVerification.class))).thenAnswer(invocation -> {
            OwnerVerification ov = invocation.getArgument(0);
            if (ov.getId() == null) ov.setId(UUID.randomUUID());
            return ov;
        });

        // Low face match score (< 0.85)
        when(faceMatchPort.matchFaces(any(UUID.class), any(UUID.class))).thenReturn(0.50);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("status", "SUCCESS");
        when(identityVerificationPort.verifyIdentity(eq(userId), any())).thenReturn(new VerificationMetadata(details));

        // Execute
        OwnerVerification result = submissionService.submitVerification(
                userId, 
                VerificationLevel.STANDARD, 
                VerificationProvider.JUMIO, 
                mediaIds, 
                "key_456"
        );

        // Asserts
        assertNotNull(result);
        assertEquals(VerificationStatus.PENDING, result.getVerificationStatus()); // Remains pending

        // Verify moderation case is flagged
        verify(moderationService).createCase(eq("OWNER_VERIFICATION"), eq(result.getId()), any());
    }

    @Test
    public void testSubmitVerification_ReturnsExisting_OnIdempotentDuplicate() {
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "dup_key";

        OwnerVerification existingVerification = new OwnerVerification();
        existingVerification.setId(UUID.randomUUID());
        existingVerification.setUserId(userId);
        existingVerification.setVerificationStatus(VerificationStatus.APPROVED);

        when(ownerVerificationRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingVerification));

        // Execute
        OwnerVerification result = submissionService.submitVerification(
                userId, 
                VerificationLevel.STANDARD, 
                VerificationProvider.JUMIO, 
                null, 
                idempotencyKey
        );

        // Verify existing returned immediately without running checks
        assertSame(existingVerification, result);
        verify(ownerVerificationRepository, never()).save(any());
        verify(ocrPort, never()).extractText(any());
    }
}
