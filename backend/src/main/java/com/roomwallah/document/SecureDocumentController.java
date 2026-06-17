package com.roomwallah.document;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/vault/documents")
@RequiredArgsConstructor
@Tag(name = "Digital Document Vault", description = "Secure document management with AES-256 encryption and access logging")
public class SecureDocumentController {

    private final SecureDocumentService documentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/upload")
    @Operation(summary = "Upload and encrypt a document in the vault")
    public ApiResponse<SecureDocument> upload(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String docType = payload.get("documentType");
        String name = payload.get("fileName");
        String base64Content = payload.get("content");
        byte[] fileBytes = Base64.getDecoder().decode(base64Content);
        
        String expiresAtStr = payload.get("expiresAt");
        Instant expiresAt = expiresAtStr != null ? Instant.parse(expiresAtStr) : null;
        String ip = request.getRemoteAddr();

        SecureDocument doc = documentService.uploadDocument(userId, docType, name, fileBytes, expiresAt, ip);
        return ApiResponse.success(doc, "Document uploaded and encrypted successfully");
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download and decrypt a document from the vault")
    public ApiResponse<Map<String, String>> download(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String ip = request.getRemoteAddr();
        byte[] decryptedBytes = documentService.downloadDocument(id, userId, ip);

        Map<String, String> result = new HashMap<>();
        result.put("content", Base64.getEncoder().encodeToString(decryptedBytes));
        return ApiResponse.success(result, "Document downloaded and decrypted successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a document in the vault")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String ip = request.getRemoteAddr();
        documentService.softDelete(id, userId, ip);
        return ApiResponse.success("Document deleted successfully");
    }

    @GetMapping
    @Operation(summary = "List all active documents owned by the authenticated user")
    public ApiResponse<List<SecureDocument>> list(HttpServletRequest request) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        String ip = request.getRemoteAddr();
        List<SecureDocument> docs = documentService.getOwnerDocuments(userId, userId, ip);
        return ApiResponse.success(docs, "Documents retrieved successfully");
    }
}
