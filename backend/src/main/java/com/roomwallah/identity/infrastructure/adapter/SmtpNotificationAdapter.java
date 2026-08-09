package com.roomwallah.identity.infrastructure.adapter;

import com.roomwallah.identity.domain.port.NotificationPort;
import com.roomwallah.notification.port.NotificationSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SmtpNotificationAdapter implements NotificationPort, NotificationSenderPort {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${roomwallah.mail.from:no-reply@roomwallah.co.in}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        boolean isProduction = isProductionEnvironment();

        boolean isSmtpConfigured = mailSender != null && mailHost != null && !mailHost.isBlank() && !"localhost".equalsIgnoreCase(mailHost);

        if (isProduction && !isSmtpConfigured) {
            log.error("Production SMTP host is not configured. Unable to send email to: {}", to);
            throw new IllegalStateException("Production mail service is unconfigured. Unable to send email.");
        }

        if (isSmtpConfigured) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);
                log.info("Email sent successfully via SMTP [Host: {}] - To: {}, Subject: {}", mailHost, to, subject);
            } catch (Exception e) {
                log.error("Failed to send email via SMTP [Host: {}] to {}: {}", mailHost, to, e.getMessage());
                throw new IllegalStateException("Failed to deliver verification email. Please try again later.", e);
            }
        } else {
            // Local development mock fallback
            log.info("[LOCAL DEV MAIL] To: {}, Subject: {}", to, subject);
        }
    }

    private boolean isProductionEnvironment() {
        if (activeProfile == null) {
            return false;
        }
        String profile = activeProfile.trim().toLowerCase();
        return profile.contains("prod") || profile.contains("production");
    }

    @Override
    public void sendSms(String to, String message) {
        log.info("[SMS NOTIFICATION] To: {}", to);
    }

    @Override
    public void sendPush(String userToken, String title, String body) {
        log.info("[PUSH NOTIFICATION] Token: {}, Title: {}", userToken, title);
    }

    @Override
    public void sendWhatsApp(String phone, String message) {
        log.info("[WHATSAPP NOTIFICATION] To: {}", phone);
    }
}
