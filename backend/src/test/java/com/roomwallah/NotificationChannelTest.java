package com.roomwallah;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotificationChannelTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;
    @Mock
    private NotificationPreferencesRepository preferencesRepository;
    @Mock
    private NotificationQueueItemRepository queueItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationSenderPort notificationSenderPort;

    private NotificationService notificationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(
                inAppNotificationRepository,
                preferencesRepository,
                queueItemRepository,
                userRepository,
                notificationSenderPort
        );
    }

    @Test
    public void testWhatsAppNotificationQueued() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("tenant@roomwallah.com");
        user.setPhone("+919876543210");

        NotificationPreferences prefs = NotificationPreferences.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(true) // will enable SMS and WhatsApp
                .inAppEnabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        notificationService.sendNotification(userId, "Lease Signed", "Your lease is fully signed", "LEASE_EVENT");

        // Verify that we queued both EMAIL, SMS and WHATSAPP
        verify(queueItemRepository, times(1)).save(argThat(item -> 
                "WHATSAPP".equals(item.getMessageType()) && "+919876543210".equals(item.getRecipient())
        ));
        verify(queueItemRepository, times(1)).save(argThat(item -> 
                "EMAIL".equals(item.getMessageType()) && "tenant@roomwallah.com".equals(item.getRecipient())
        ));
    }

    @Test
    public void testWhatsAppRetryAndDqFlows() throws Exception {
        UUID itemId = UUID.randomUUID();
        NotificationQueueItem item = NotificationQueueItem.builder()
                .recipient("+919876543210")
                .messageType("WHATSAPP")
                .content("Your rent is due in 3 days")
                .status("PENDING")
                .attemptCount(0)
                .nextAttemptAt(Instant.now().minusSeconds(10))
                .build();
        item.setId(itemId);

        when(queueItemRepository.findByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(anyList(), any(Instant.class)))
                .thenReturn(List.of(item));

        // 1. First attempt fails
        doThrow(new RuntimeException("WhatsApp provider offline")).when(notificationSenderPort).sendWhatsApp(anyString(), anyString());

        notificationService.processRetryQueue();

        assertEquals("FAILED", item.getStatus());
        assertEquals(1, item.getAttemptCount());
        assertNotNull(item.getErrorLog());
        assertTrue(item.getNextAttemptAt().isAfter(Instant.now()));

        // 2. Reach max attempts (5) and transition to DLQ
        item.setAttemptCount(4);
        item.setNextAttemptAt(Instant.now().minusSeconds(10));

        notificationService.processRetryQueue();

        assertEquals("DLQ", item.getStatus());
        assertEquals(5, item.getAttemptCount());
    }
}
