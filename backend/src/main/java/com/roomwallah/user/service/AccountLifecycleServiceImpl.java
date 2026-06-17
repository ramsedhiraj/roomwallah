package com.roomwallah.user.service;

import com.roomwallah.identity.domain.port.AuditPort;
import com.roomwallah.identity.domain.port.EventPublisherPort;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.event.AccountDeactivatedEvent;
import com.roomwallah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLifecycleServiceImpl implements AccountLifecycleService {

    private final UserRepository userRepository;
    private final EventPublisherPort eventPublisher;
    private final AuditPort auditPort;

    @Override
    @Transactional
    public void deactivateAccount(User user) {
        log.info("Processing account deactivation request for user: {}", user.getEmail());

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Account is already deactivated");
        }

        user.setStatus(AccountStatus.DISABLED);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);

        // Publish event
        AccountDeactivatedEvent event = AccountDeactivatedEvent.builder()
                .userId(user.getId())
                .deactivatedAt(Instant.now())
                .build();
        eventPublisher.publish(event);

        // Audit log
        auditPort.log(
                "USER_ACCOUNT_DEACTIVATION",
                user.getId().toString(),
                "0.0.0.0",
                Map.of(
                        "email", user.getEmail()
                )
        );
    }
}
