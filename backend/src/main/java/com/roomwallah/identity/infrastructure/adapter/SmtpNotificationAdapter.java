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

    @Value("${roomwallah.mail.from:${spring.mail.username:no-reply@roomwallah.co.in}}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, null);
    }

    @Override
    public void sendEmail(String to, String subject, String plainTextBody, String htmlBody) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        boolean isProduction = isProductionEnvironment();

        boolean hasMailHost = mailHost != null && !mailHost.isBlank();
        boolean isSmtpConfigured = mailSender != null && hasMailHost;

        if (isProduction && !isSmtpConfigured) {
            log.error("Production SMTP host is not configured. Unable to send email to: {}", to);
            throw new IllegalStateException("Production mail service is unconfigured. Unable to send email.");
        }

        if (isSmtpConfigured) {
            try {
                jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = 
                        new org.springframework.mail.javamail.MimeMessageHelper(
                                mimeMessage, 
                                org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                                java.nio.charset.StandardCharsets.UTF_8.name());

                String senderFrom = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "no-reply@roomwallah.co.in";
                try {
                    helper.setFrom(senderFrom, "RoomWallah");
                } catch (Exception e) {
                    helper.setFrom(senderFrom);
                }
                helper.setTo(to);
                helper.setSubject(subject);

                if (htmlBody != null && !htmlBody.isBlank()) {
                    helper.setText(plainTextBody != null ? plainTextBody : "", htmlBody);
                } else {
                    helper.setText(plainTextBody != null ? plainTextBody : "", false);
                }

                mailSender.send(mimeMessage);
                log.info("Email sent successfully via SMTP [Host: {}] - To: {}, Subject: {}", mailHost, to, subject);
            } catch (Exception e) {
                log.error("Failed to send email via SMTP [Host: {}] to {}: {}", mailHost, to, e.getMessage());
                if (isProduction) {
                    throw new IllegalStateException("Failed to deliver transactional email. Please try again later.", e);
                } else {
                    log.info("[LOCAL DEV MAIL FALLBACK - SMTP Connection Failed] To: {}, Subject: {}\nPlain Text:\n{}\n", 
                            to, subject, plainTextBody);
                }
            }
        } else {
            // Local development mock fallback
            log.info("[LOCAL DEV MAIL] To: {}, Subject: {}\nPlain Text:\n{}\n", to, subject, plainTextBody);
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
