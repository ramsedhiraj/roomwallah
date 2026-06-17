package com.roomwallah.identity.infrastructure.event;

import com.roomwallah.identity.domain.event.UserLoggedInEvent;
import com.roomwallah.identity.domain.event.UserLoggedOutEvent;
import com.roomwallah.user.event.UserRegisteredEvent;
import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final NotificationPort notificationPort;
    private final AuditPort auditPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.debug("Event Listener - Handling UserRegisteredEvent for user: {}", event.getEmail());
        
        auditPort.log(
                "USER_REGISTRATION", 
                event.getUserId().toString(), 
                "0.0.0.0", 
                Map.of(
                    "email", event.getEmail(),
                    "role", event.getRole().name(),
                    "timestamp", event.getRegisteredAt().toString()
                )
        );

        notificationPort.sendEmail(
                event.getEmail(), 
                "Welcome to RoomWallah!", 
                "Hello " + event.getFullName() + ",\nYour registration as a " + event.getRole() + " was successful!"
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLoggedIn(UserLoggedInEvent event) {
        log.debug("Event Listener - Handling UserLoggedInEvent for user: {}", event.getEmail());

        auditPort.log(
                "USER_LOGIN", 
                event.getUserId().toString(), 
                event.getIpAddress(), 
                Map.of(
                    "sessionId", event.getSessionId().toString(),
                    "email", event.getEmail(),
                    "device", event.getDeviceName(),
                    "timestamp", event.getLoggedInAt().toString()
                )
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLoggedOut(UserLoggedOutEvent event) {
        log.debug("Event Listener - Handling UserLoggedOutEvent for session: {}", event.getSessionId());

        auditPort.log(
                "USER_LOGOUT", 
                event.getUserId().toString(), 
                "0.0.0.0", 
                Map.of(
                    "sessionId", event.getSessionId().toString(),
                    "timestamp", event.getLoggedOutAt().toString()
                )
        );
    }
}
