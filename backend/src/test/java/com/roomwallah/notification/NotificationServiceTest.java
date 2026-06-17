package com.roomwallah.notification;

import com.roomwallah.notification.domain.InAppNotification;
import com.roomwallah.notification.domain.NotificationPreferences;
import com.roomwallah.notification.domain.NotificationQueueItem;
import com.roomwallah.notification.port.NotificationSenderPort;
import com.roomwallah.notification.repository.InAppNotificationRepository;
import com.roomwallah.notification.repository.NotificationPreferencesRepository;
import com.roomwallah.notification.repository.NotificationQueueItemRepository;
import com.roomwallah.notification.service.NotificationService;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;

    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Mock
    private NotificationQueueItemRepository notificationQueueItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationSenderPort notificationSenderPort;

    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(
                inAppNotificationRepository, notificationPreferencesRepository,
                notificationQueueItemRepository, userRepository, notificationSenderPort
        );
    }

    @Test
    public void testSendNotification_PreferencesEnforced() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setEmail("test@roomwallah.com");
        u.setPhone("9999999999");

        NotificationPreferences prefs = NotificationPreferences.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(false)
                .inAppEnabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(notificationPreferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        notificationService.sendNotification(userId, "Test Title", "Test Message", "SYSTEM");

        verify(inAppNotificationRepository, times(1)).save(any(InAppNotification.class));
        verify(notificationQueueItemRepository, times(1)).save(argThat(item -> "EMAIL".equals(item.getMessageType())));
        verify(notificationQueueItemRepository, never()).save(argThat(item -> "SMS".equals(item.getMessageType())));
    }

    @Test
    public void testProcessRetryQueue_SuccessAndDLQ() {
        NotificationQueueItem item1 = NotificationQueueItem.builder()
                .recipient("test@roomwallah.com")
                .messageType("EMAIL")
                .content("Body")
                .status("PENDING")
                .attemptCount(0)
                .build();

        NotificationQueueItem item2 = NotificationQueueItem.builder()
                .recipient("9999999999")
                .messageType("SMS")
                .content("Body")
                .status("FAILED")
                .attemptCount(4) // One more to DLQ
                .build();

        List<NotificationQueueItem> items = new ArrayList<>(List.of(item1, item2));
        when(notificationQueueItemRepository.findByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(anyList(), any(Instant.class)))
                .thenReturn(items);

        // Make SMS fail
        doThrow(new RuntimeException("SMS Gateway Down")).when(notificationSenderPort).sendSms(anyString(), anyString());

        notificationService.processRetryQueue();

        assertEquals("SENT", item1.getStatus());
        assertEquals("DLQ", item2.getStatus());
        verify(notificationQueueItemRepository, times(1)).save(item1);
        verify(notificationQueueItemRepository, times(1)).save(item2);
    }
}
