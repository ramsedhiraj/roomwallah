package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("Notification Port [EMAIL] - To: {}, Subject: {}, Body: {}", to, subject, body);
    }

    @Override
    public void sendSms(String to, String message) {
        log.info("Notification Port [SMS] - To: {}, Message: {}", to, message);
    }
}
