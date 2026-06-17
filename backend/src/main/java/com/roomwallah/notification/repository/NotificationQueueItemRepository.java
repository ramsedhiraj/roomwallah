package com.roomwallah.notification.repository;

import com.roomwallah.notification.domain.NotificationQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationQueueItemRepository extends JpaRepository<NotificationQueueItem, UUID> {
    List<NotificationQueueItem> findByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(
            List<String> statuses, Instant time
    );
}
