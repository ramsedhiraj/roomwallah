package com.roomwallah.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainOutboxEventRepository extends JpaRepository<DomainOutboxEvent, UUID> {

    Optional<DomainOutboxEvent> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM DomainOutboxEvent e WHERE e.status = 'PENDING' AND e.nextAttemptAt <= :now ORDER BY e.createdAt ASC")
    List<DomainOutboxEvent> findPendingEvents(@Param("now") Instant now);
}
