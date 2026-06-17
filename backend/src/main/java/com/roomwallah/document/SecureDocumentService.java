package com.roomwallah.document;

import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecureDocumentService {

    private final SecureDocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;

    private static final String AES_KEY = "RoomWallahSecretKeySecuringFiles";

    @Transactional
    public SecureDocument uploadDocument(
            UUID ownerId,
            String docType,
            String fileName,
            byte[] fileBytes,
            Instant expiresAt,
            String ipAddress
    ) {
        log.info("Encrypting and uploading secure document: {} for owner: {}", fileName, ownerId);

        byte[] processedBytes;
        try {
            processedBytes = encryptAes(fileBytes);
        } catch (Exception e) {
            log.error("AES-256 encryption failed for document {}", fileName, e);
            throw new SecurityException("Enforced encryption service unavailable. Upload aborted securely.", e);
        }

        String simulatedFileKey = Base64.getEncoder().encodeToString(processedBytes);

        SecureDocument doc = SecureDocument.builder()
                .ownerId(ownerId)
                .documentType(docType)
                .fileName(fileName)
                .fileKey(simulatedFileKey)
                .isEncrypted(true)
                .isDeleted(false)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        doc = documentRepository.save(doc);

        logAccess(doc.getId(), ownerId, "WRITE", ipAddress);

        return doc;
    }

    @Transactional
    public byte[] downloadDocument(UUID documentId, UUID accessorId, String ipAddress) {
        log.info("Downloading and decrypting document: {} by accessor: {}", documentId, accessorId);
        SecureDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.isDeleted()) {
            throw new IllegalStateException("Document has been deleted");
        }

        byte[] rawBytes = Base64.getDecoder().decode(doc.getFileKey());
        byte[] decryptedBytes;

        if (doc.isEncrypted()) {
            try {
                decryptedBytes = decryptAes(rawBytes);
            } catch (Exception e) {
                log.error("Failed to decrypt AES document", e);
                throw new RuntimeException("Decryption error", e);
            }
        } else {
            decryptedBytes = Base64.getDecoder().decode(rawBytes);
        }

        logAccess(doc.getId(), accessorId, "DOWNLOAD", ipAddress);

        return decryptedBytes;
    }

    @Transactional
    public void softDelete(UUID documentId, UUID accessorId, String ipAddress) {
        log.info("Soft deleting document: {}", documentId);
        SecureDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getOwnerId().equals(accessorId)) {
            throw new SecurityException("Unauthorized delete action");
        }

        doc.setDeleted(true);
        doc.setUpdatedAt(Instant.now());
        documentRepository.save(doc);

        logAccess(doc.getId(), accessorId, "DELETE", ipAddress);
    }

    public List<SecureDocument> getOwnerDocuments(UUID ownerId, UUID accessorId, String ipAddress) {
        List<SecureDocument> docs = documentRepository.findByOwnerIdAndIsDeletedFalse(ownerId);
        for (SecureDocument doc : docs) {
            logAccess(doc.getId(), accessorId, "READ", ipAddress);
        }
        return docs;
    }

    private void logAccess(UUID docId, UUID accessorId, String type, String ip) {
        DocumentAccessLog logEntry = DocumentAccessLog.builder()
                .documentId(docId)
                .accessorId(accessorId)
                .accessType(type)
                .accessedAt(Instant.now())
                .ipAddress(ip)
                .build();
        accessLogRepository.save(logEntry);
    }

    private byte[] encryptAes(byte[] data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private byte[] decryptAes(byte[] data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }
}
