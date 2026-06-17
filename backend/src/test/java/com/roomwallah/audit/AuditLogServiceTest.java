package com.roomwallah.audit;

import com.roomwallah.audit.domain.AuditLog;
import com.roomwallah.audit.repository.AuditLogRepository;
import com.roomwallah.audit.service.AuditLogService;
import com.roomwallah.audit.util.HashUtils;
import com.roomwallah.common.observability.CorrelationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogService = new AuditLogService(auditLogRepository);
        CorrelationContext.clear();
    }

    @Test
    public void testLogSync_ChainCreatedSuccessfully() {
        List<AuditLog> dbLogs = new ArrayList<>();
        when(auditLogRepository.findAll()).thenReturn(dbLogs);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setCreatedAt(Instant.now());
            dbLogs.add(log);
            return log;
        });

        CorrelationContext.set("test-corr-id");

        auditLogService.logSync("CREATE_BOOKING", "developer", "Booking", "b123", "SUCCESS", "{}", null);
        auditLogService.logSync("APPROVE_BOOKING", "owner", "Booking", "b123", "SUCCESS", "{}", null);

        assertEquals(2, dbLogs.size());

        AuditLog log1 = dbLogs.get(0);
        AuditLog log2 = dbLogs.get(1);

        assertEquals("GENESIS", log1.getPreviousHash());
        assertEquals(log1.getCurrentHash(), log2.getPreviousHash());
        assertEquals("test-corr-id", log1.getCorrelationId());
        assertEquals("test-corr-id", log2.getCorrelationId());
    }

    @Test
    public void testVerifyLogChain_SuccessAndTamperedEditDetection() {
        List<AuditLog> dbLogs = new ArrayList<>();

        UUID id1 = UUID.randomUUID();
        String prevHash1 = "GENESIS";
        String hmacKey1 = "GENESIS_HMAC_SALT_KEY";
        String data1 = id1.toString() + "CREATE" + "operator" + "" + "" + "SUCCESS" + "" + "" + "corr1" + prevHash1;
        String hash1 = HashUtils.hmacSha256(data1, hmacKey1);

        AuditLog log1 = AuditLog.builder()
                .action("CREATE")
                .operator("operator")
                .status("SUCCESS")
                .correlationId("corr1")
                .previousHash(prevHash1)
                .currentHash(hash1)
                .integrityStatus("VALID")
                .build();
        log1.setId(id1);
        log1.setCreatedAt(Instant.now().minusSeconds(10));

        UUID id2 = UUID.randomUUID();
        String data2 = id2.toString() + "UPDATE" + "operator" + "" + "" + "SUCCESS" + "" + "" + "corr1" + hash1;
        String hash2 = HashUtils.hmacSha256(data2, hash1);

        AuditLog log2 = AuditLog.builder()
                .action("UPDATE")
                .operator("operator")
                .status("SUCCESS")
                .correlationId("corr1")
                .previousHash(hash1)
                .currentHash(hash2)
                .integrityStatus("VALID")
                .build();
        log2.setId(id2);
        log2.setCreatedAt(Instant.now());

        dbLogs.add(log1);
        dbLogs.add(log2);

        when(auditLogRepository.findAll()).thenReturn(dbLogs);

        // Chain is valid initially
        assertTrue(auditLogService.verifyLogChain());

        // Tamper with log1
        log1.setAction("DELETE");

        // Verifier must detect tampering and return false
        assertFalse(auditLogService.verifyLogChain());
    }
}
