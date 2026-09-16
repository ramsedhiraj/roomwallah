package com.roomwallah.identity.domain.port;

public interface NotificationPort {
    void sendEmail(String to, String subject, String body);
    default void sendEmail(String to, String subject, String plainTextBody, String htmlBody) {
        sendEmail(to, subject, htmlBody != null ? htmlBody : plainTextBody);
    }
    void sendSms(String to, String message);
}
