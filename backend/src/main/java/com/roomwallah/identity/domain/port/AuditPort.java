package com.roomwallah.identity.domain.port;

import java.util.Map;

public interface AuditPort {
    void log(String action, String userId, String ipAddress, Map<String, Object> details);
}
