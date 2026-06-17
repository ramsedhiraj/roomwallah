package com.roomwallah.notification.port;

public interface NotificationSenderPort {
    void sendEmail(String to, String title, String body);
    void sendSms(String phone, String message);
    void sendPush(String userToken, String title, String body);
    void sendWhatsApp(String phone, String message);
}
