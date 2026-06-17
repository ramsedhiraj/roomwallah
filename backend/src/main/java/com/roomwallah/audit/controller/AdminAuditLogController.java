package com.roomwallah.audit.controller;

import com.roomwallah.audit.domain.AuditLog;
import com.roomwallah.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String correlationId
    ) {
        List<AuditLog> logs = auditLogService.getAllAuditLogs(operator, action, correlationId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        boolean valid = auditLogService.verifyLogChain();
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        result.put("message", valid ? "Audit log chain is cryptographically secure." : "Audit log chain integrity compromised!");
        return ResponseEntity.ok(result);
    }
}
