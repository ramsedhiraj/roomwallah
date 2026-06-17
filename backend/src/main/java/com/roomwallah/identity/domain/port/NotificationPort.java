package com.roomwallah.identity.domain.port;

public interface NotificationPort {
    void sendEmail(String to, String subject, String body);
    void sendSms(String to, String message);
}
