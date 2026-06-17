package com.roomwallah.notification.adapter;

import com.roomwallah.notification.port.NotificationSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockNotificationSenderAdapter implements NotificationSenderPort {

    @Override
    public void sendEmail(String to, String title, String body) {
        log.info("[MOCK EMAIL SENDER] Sending to: '{}', title: '{}', body: '{}'", to, title, body);
    }

    @Override
    public void sendSms(String phone, String message) {
        log.info("[MOCK SMS SENDER] Sending to: '{}', message: '{}'", phone, message);
    }

    @Override
    public void sendPush(String userToken, String title, String body) {
        log.info("[MOCK PUSH SENDER] Sending to token: '{}', title: '{}', body: '{}'", userToken, title, body);
    }

    @Override
    public void sendWhatsApp(String phone, String message) {
        log.info("[MOCK WHATSAPP SENDER] Sending to: '{}', message: '{}'", phone, message);
    }
}
