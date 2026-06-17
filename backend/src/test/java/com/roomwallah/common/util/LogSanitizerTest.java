package com.roomwallah.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogSanitizerTest {

    @Test
    public void testRedact_SensitiveDataRedacted() {
        String logMessage1 = "Connecting with password: \"mysecret123\", apiKey: \"mykey123\", token: \"mytoken123\"";
        String redacted1 = LogSanitizer.redact(logMessage1);
        
        assertTrue(redacted1.contains("password=[REDACTED]"));
        assertTrue(redacted1.contains("apiKey=[REDACTED]"));
        assertTrue(redacted1.contains("token=[REDACTED]"));

        String logMessage2 = "Used raw key rw_key_abcdef12345 and JWT token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c to authenticate.";
        String redacted2 = LogSanitizer.redact(logMessage2);
        
        assertTrue(redacted2.contains("[REDACTED_API_KEY]"));
        assertTrue(redacted2.contains("[REDACTED_JWT]"));
    }
}
