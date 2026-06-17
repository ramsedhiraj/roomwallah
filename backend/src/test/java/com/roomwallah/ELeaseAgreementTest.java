package com.roomwallah;

import com.roomwallah.agreement.LeaseAgreement;
import com.roomwallah.agreement.LeaseAgreementRepository;
import com.roomwallah.agreement.LeaseAgreementService;
import com.roomwallah.agreement.Signature;
import com.roomwallah.document.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ELeaseAgreementTest {

    @Mock
    private LeaseAgreementRepository agreementRepository;

    @Mock
    private SecureDocumentRepository documentRepository;

    @Mock
    private DocumentAccessLogRepository accessLogRepository;

    private LeaseAgreementService leaseAgreementService;
    private SecureDocumentService secureDocumentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        leaseAgreementService = new LeaseAgreementService(agreementRepository);
        secureDocumentService = new SecureDocumentService(documentRepository, accessLogRepository);
    }

    @Test
    public void testLeaseLifecycleTransitions() {
        UUID propertyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();

        LeaseAgreement agreement = LeaseAgreement.builder()
                .id(agreementId)
                .propertyId(propertyId)
                .tenantId(tenantId)
                .ownerId(ownerId)
                .rentAmount(BigDecimal.valueOf(15000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .agreementContent("RoomWallah standard broker-free lease contract v1")
                .status("PENDING_SIGNATURE")
                .signatures(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));
        when(agreementRepository.save(any(LeaseAgreement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Sign by Tenant
        LeaseAgreement tenantSigned = leaseAgreementService.signAgreement(
                agreementId, tenantId, "hash-tenant-123", "192.168.1.1", "fingerprint-tenant"
        );

        assertEquals("PENDING_SIGNATURE", tenantSigned.getStatus());
        assertEquals(1, tenantSigned.getSignatures().size());
        assertEquals(tenantId, tenantSigned.getSignatures().get(0).getUserId());

        // 2. Sign by Owner
        LeaseAgreement fullySigned = leaseAgreementService.signAgreement(
                agreementId, ownerId, "hash-owner-123", "192.168.1.2", "fingerprint-owner"
        );

        // Status must be SIGNED when both sign
        assertEquals("SIGNED", fullySigned.getStatus());
        assertEquals(2, fullySigned.getSignatures().size());
    }

    @Test
    public void testUnauthorizedPartySigningLease() {
        UUID propertyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();
        UUID hackerId = UUID.randomUUID();

        LeaseAgreement agreement = LeaseAgreement.builder()
                .id(agreementId)
                .propertyId(propertyId)
                .tenantId(tenantId)
                .ownerId(ownerId)
                .signatures(new ArrayList<>())
                .build();

        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        assertThrows(SecurityException.class, () -> {
            leaseAgreementService.signAgreement(agreementId, hackerId, "hash", "127.0.0.1", "fp");
        });
    }

    @Test
    public void testDocumentVaultEncryptionDecryptionAndLogs() {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        String originalContent = "Confidential government ID verification documents";
        byte[] originalBytes = originalContent.getBytes();

        when(documentRepository.save(any(SecureDocument.class))).thenAnswer(invocation -> {
            SecureDocument d = invocation.getArgument(0);
            d.setId(docId);
            return d;
        });

        // 1. Upload/Encrypt
        SecureDocument doc = secureDocumentService.uploadDocument(
                ownerId, "KYC", "passport.pdf", originalBytes, Instant.now().plusSeconds(3600), "127.0.0.1"
        );

        assertNotNull(doc.getId());
        assertTrue(doc.isEncrypted());
        assertNotEquals(originalContent, doc.getFileKey());
        verify(accessLogRepository, times(1)).save(argThat(log -> 
                "WRITE".equals(log.getAccessType()) && ownerId.equals(log.getAccessorId())
        ));

        // 2. Download/Decrypt
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        byte[] decryptedBytes = secureDocumentService.downloadDocument(docId, ownerId, "127.0.0.1");

        String decryptedContent = new String(decryptedBytes);
        assertEquals(originalContent, decryptedContent);
        verify(accessLogRepository, times(1)).save(argThat(log -> 
                "DOWNLOAD".equals(log.getAccessType()) && ownerId.equals(log.getAccessorId())
        ));
    }
}
