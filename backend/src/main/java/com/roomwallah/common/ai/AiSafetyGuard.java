package com.roomwallah.common.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AiSafetyGuard {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore previous instructions|system prompt|dan mode|jailbreak|sql injection|drop table|select \\*)"
    );

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|password|secret|bearer [a-zA-Z0-9_\\-\\.]+)"
    );

    public void checkAuthorization(UUID userId, String action) {
        log.info("Checking AI authorization for user: {}, action: {}", userId, action);
        if (userId != null && userId.toString().contains("blocked")) {
            throw new SecurityException("User is blocked from accessing AI services");
        }
    }

    public String validateAndFilterInput(String input, UUID userId) {
        if (input == null) return "";
        log.info("Auditing AI input for user: {}", userId);

        if (INJECTION_PATTERN.matcher(input).find()) {
            log.warn("Prompt injection detected in input from user: {}", userId);
            throw new IllegalArgumentException("Potential prompt injection attempt detected.");
        }

        return input;
    }

    public String maskSecrets(String output) {
        if (output == null) return "";
        return SECRET_PATTERN.matcher(output).replaceAll("[REDACTED_SECRET]");
    }
}
