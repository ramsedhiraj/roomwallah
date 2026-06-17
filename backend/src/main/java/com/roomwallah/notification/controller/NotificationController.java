package com.roomwallah.notification.controller;

import com.roomwallah.identity.infrastructure.provider.UserPrincipal;
import com.roomwallah.notification.domain.InAppNotification;
import com.roomwallah.notification.domain.NotificationPreferences;
import com.roomwallah.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private UUID getLoggedInUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.user().getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<InAppNotification>> getInbox() {
        UUID userId = getLoggedInUserId();
        List<InAppNotification> inbox = notificationService.getInAppNotifications(userId);
        return ResponseEntity.ok(inbox);
    }

    @PostMapping("/inbox/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferences> getPreferences() {
        UUID userId = getLoggedInUserId();
        NotificationPreferences prefs = notificationService.getPreferences(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferences> updatePreferences(
            @RequestParam boolean email,
            @RequestParam boolean sms,
            @RequestParam boolean inApp
    ) {
        UUID userId = getLoggedInUserId();
        NotificationPreferences prefs = notificationService.updatePreferences(userId, email, sms, inApp);
        return ResponseEntity.ok(prefs);
    }
}
