package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.AuditPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LoggingAuditAdapter implements AuditPort {

    @Override
    public void log(String action, String userId, String ipAddress, Map<String, Object> details) {
        log.info("AUDIT - Action: [{}], User: [{}], IP: [{}], Context: {}", action, userId, ipAddress, details);
    }
}
