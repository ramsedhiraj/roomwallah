package com.roomwallah.notification.service;

import com.roomwallah.notification.domain.InAppNotification;
import com.roomwallah.notification.domain.NotificationPreferences;
import com.roomwallah.notification.domain.NotificationQueueItem;
import com.roomwallah.notification.port.NotificationSenderPort;
import com.roomwallah.notification.repository.InAppNotificationRepository;
import com.roomwallah.notification.repository.NotificationPreferencesRepository;
import com.roomwallah.notification.repository.NotificationQueueItemRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final NotificationQueueItemRepository notificationQueueItemRepository;
    private final UserRepository userRepository;
    private final NotificationSenderPort notificationSenderPort;

    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public void sendNotification(UUID userId, String title, String message, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot send notification. User not found: {}", userId);
            return;
        }

        NotificationPreferences prefs = notificationPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreferences newPrefs = NotificationPreferences.builder()
                            .userId(userId)
                            .emailEnabled(true)
                            .smsEnabled(true)
                            .inAppEnabled(true)
                            .build();
                    return notificationPreferencesRepository.save(newPrefs);
                });

        if (prefs.isInAppEnabled()) {
            InAppNotification inApp = InAppNotification.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .status("UNREAD")
                    .notificationType(type)
                    .build();
            inAppNotificationRepository.save(inApp);
        }

        if (prefs.isEmailEnabled() && user.getEmail() != null) {
            NotificationQueueItem emailItem = NotificationQueueItem.builder()
                    .recipient(user.getEmail())
                    .messageType("EMAIL")
                    .title(title)
                    .content(message)
                    .status("PENDING")
                    .attemptCount(0)
                    .nextAttemptAt(Instant.now())
                    .build();
            notificationQueueItemRepository.save(emailItem);
        }

        if (prefs.isSmsEnabled() && user.getPhone() != null) {
            NotificationQueueItem smsItem = NotificationQueueItem.builder()
                    .recipient(user.getPhone())
                    .messageType("SMS")
                    .content(message)
                    .status("PENDING")
                    .attemptCount(0)
                    .nextAttemptAt(Instant.now())
                    .build();
            notificationQueueItemRepository.save(smsItem);

            NotificationQueueItem waItem = NotificationQueueItem.builder()
                    .recipient(user.getPhone())
                    .messageType("WHATSAPP")
                    .content(message)
                    .status("PENDING")
                    .attemptCount(0)
                    .nextAttemptAt(Instant.now())
                    .build();
            notificationQueueItemRepository.save(waItem);
        }
    }

    @Transactional(readOnly = true)
    public List<InAppNotification> getInAppNotifications(UUID userId) {
        return inAppNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID id) {
        inAppNotificationRepository.findById(id).ifPresent(n -> {
            n.setStatus("READ");
            inAppNotificationRepository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public NotificationPreferences getPreferences(UUID userId) {
        return notificationPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreferences.builder()
                        .userId(userId)
                        .emailEnabled(true)
                        .smsEnabled(true)
                        .inAppEnabled(true)
                        .build());
    }

    @Transactional
    public NotificationPreferences updatePreferences(UUID userId, boolean email, boolean sms, boolean inApp) {
        NotificationPreferences prefs = notificationPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreferences newPrefs = NotificationPreferences.builder().userId(userId).build();
                    newPrefs.setVersion(0L);
                    return newPrefs;
                });
        prefs.setEmailEnabled(email);
        prefs.setSmsEnabled(sms);
        prefs.setInAppEnabled(inApp);
        return notificationPreferencesRepository.save(prefs);
    }

    @Transactional
    public void processRetryQueue() {
        Instant now = Instant.now();
        List<NotificationQueueItem> items = notificationQueueItemRepository.findByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                List.of("PENDING", "FAILED"), now
        );

        if (items.isEmpty()) {
            return;
        }

        log.info("Processing {} items from notification retry queue...", items.size());

        for (NotificationQueueItem item : items) {
            try {
                if ("EMAIL".equalsIgnoreCase(item.getMessageType())) {
                    notificationSenderPort.sendEmail(item.getRecipient(), item.getTitle(), item.getContent());
                } else if ("SMS".equalsIgnoreCase(item.getMessageType())) {
                    notificationSenderPort.sendSms(item.getRecipient(), item.getContent());
                } else if ("PUSH".equalsIgnoreCase(item.getMessageType())) {
                    notificationSenderPort.sendPush(item.getRecipient(), item.getTitle(), item.getContent());
                } else if ("WHATSAPP".equalsIgnoreCase(item.getMessageType())) {
                    notificationSenderPort.sendWhatsApp(item.getRecipient(), item.getContent());
                } else {
                    throw new IllegalArgumentException("Unknown message type: " + item.getMessageType());
                }

                item.setStatus("SENT");
                item.setErrorLog(null);
            } catch (Exception e) {
                log.error("Failed to send notification queue item: {}", item.getId(), e);
                item.setAttemptCount(item.getAttemptCount() + 1);
                item.setErrorLog(e.getMessage());

                if (item.getAttemptCount() >= MAX_ATTEMPTS) {
                    item.setStatus("DLQ");
                    log.error("Notification queue item {} moved to Dead-Letter Queue (DLQ) after {} failed attempts.", item.getId(), item.getAttemptCount());
                } else {
                    item.setStatus("FAILED");
                    long delayMinutes = (long) Math.pow(2, item.getAttemptCount());
                    item.setNextAttemptAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
                }
            }
            notificationQueueItemRepository.save(item);
        }
    }
}
