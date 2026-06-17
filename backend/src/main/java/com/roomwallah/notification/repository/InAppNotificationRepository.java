package com.roomwallah.notification.repository;

import com.roomwallah.notification.domain.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {
    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
