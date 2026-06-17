package com.roomwallah.audit.service;

import com.roomwallah.audit.domain.AuditLog;
import com.roomwallah.audit.repository.AuditLogRepository;
import com.roomwallah.audit.util.HashUtils;
import com.roomwallah.common.observability.CorrelationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void logAsync(String action, String operator, String targetEntity, String targetEntityId, String status, String payload, String errorMessage) {
        logSync(action, operator, targetEntity, targetEntityId, status, payload, errorMessage);
    }

    @Transactional
    public synchronized void logSync(String action, String operator, String targetEntity, String targetEntityId, String status, String payload, String errorMessage) {
        try {
            List<AuditLog> allLogs = auditLogRepository.findAll();
            String prevHash = "GENESIS";
            String hmacKey = "GENESIS_HMAC_SALT_KEY";
            if (!allLogs.isEmpty()) {
                AuditLog latest = allLogs.stream()
                        .max(Comparator.comparing(AuditLog::getCreatedAt))
                        .orElse(null);
                if (latest != null) {
                    prevHash = latest.getCurrentHash();
                    hmacKey = latest.getCurrentHash();
                }
            }

            UUID id = UUID.randomUUID();
            String correlationId = CorrelationContext.get();
            String payloadStr = payload != null ? payload : "";
            String errorMsgStr = errorMessage != null ? errorMessage : "";
            String targetEntStr = targetEntity != null ? targetEntity : "";
            String targetEntIdStr = targetEntityId != null ? targetEntityId : "";

            String data = id.toString() +
                    action +
                    operator +
                    targetEntStr +
                    targetEntIdStr +
                    status +
                    payloadStr +
                    errorMsgStr +
                    (correlationId != null ? correlationId : "") +
                    prevHash;

            String currentHash = HashUtils.hmacSha256(data, hmacKey);

            AuditLog logEntry = AuditLog.builder()
                    .action(action)
                    .operator(operator)
                    .targetEntity(targetEntity)
                    .targetEntityId(targetEntityId)
                    .status(status)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .correlationId(correlationId)
                    .previousHash(prevHash)
                    .currentHash(currentHash)
                    .chainVersion(1)
                    .integrityStatus("VALID")
                    .build();
            logEntry.setId(id);
            logEntry.setVersion(0L);

            auditLogRepository.save(logEntry);
            log.debug("Saved HMAC-SHA256 chained audit log: action={}, operator={}, correlationId={}, hash={}", action, operator, correlationId, currentHash);
        } catch (Exception e) {
            log.error("Failed to save audit log sync", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs(String operator, String action, String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return auditLogRepository.findByCorrelationId(correlationId);
        }
        if (operator != null && !operator.isBlank()) {
            return auditLogRepository.findByOperator(operator);
        }
        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByAction(action);
        }
        return auditLogRepository.findAll();
    }

    @Transactional
    public boolean verifyLogChain() {
        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt))
                .toList();

        String expectedPrevHash = "GENESIS";
        String hmacKey = "GENESIS_HMAC_SALT_KEY";
        boolean valid = true;

        for (AuditLog logEntry : logs) {
            boolean currentRecordValid = true;

            if (!expectedPrevHash.equals(logEntry.getPreviousHash())) {
                log.error("Integrity check failed: Log ID {} expects previousHash {}, but got {}", 
                        logEntry.getId(), expectedPrevHash, logEntry.getPreviousHash());
                currentRecordValid = false;
                valid = false;
            }

            String correlationId = logEntry.getCorrelationId();
            String payloadStr = logEntry.getPayload() != null ? logEntry.getPayload() : "";
            String errorMsgStr = logEntry.getErrorMessage() != null ? logEntry.getErrorMessage() : "";
            String targetEntStr = logEntry.getTargetEntity() != null ? logEntry.getTargetEntity() : "";
            String targetEntIdStr = logEntry.getTargetEntityId() != null ? logEntry.getTargetEntityId() : "";

            String data = logEntry.getId().toString() +
                    logEntry.getAction() +
                    logEntry.getOperator() +
                    targetEntStr +
                    targetEntIdStr +
                    logEntry.getStatus() +
                    payloadStr +
                    errorMsgStr +
                    (correlationId != null ? correlationId : "") +
                    logEntry.getPreviousHash();

            String computedHash = HashUtils.hmacSha256(data, hmacKey);
            if (!computedHash.equals(logEntry.getCurrentHash())) {
                log.error("Integrity check failed: Log ID {} computed hash {}, but stored hash is {}", 
                        logEntry.getId(), computedHash, logEntry.getCurrentHash());
                currentRecordValid = false;
                valid = false;
            }

            String newStatus = currentRecordValid ? "VALID" : "CORRUPTED";
            if (!newStatus.equals(logEntry.getIntegrityStatus())) {
                logEntry.setIntegrityStatus(newStatus);
                auditLogRepository.save(logEntry);
            }

            expectedPrevHash = logEntry.getCurrentHash();
            hmacKey = logEntry.getCurrentHash();
        }
        return valid;
    }
}
