package com.roomwallah.common.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private LogSanitizer() {}

    private static final Pattern SENSITIVE_PROPERTY_PATTERN = Pattern.compile("(?i)(password|secret|rawKey|apiKey|jwt|token|authorization)\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,]+)");
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJhbGciOi[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+");
    private static final Pattern PARTNER_KEY_PATTERN = Pattern.compile("rw_key_[A-Za-z0-9-_]+");

    public static String redact(String message) {
        if (message == null) {
            return null;
        }

        String sanitized = SENSITIVE_PROPERTY_PATTERN.matcher(message).replaceAll("$1=[REDACTED]");
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");
        sanitized = PARTNER_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_API_KEY]");

        return sanitized;
    }
}
