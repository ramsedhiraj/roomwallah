package com.roomwallah.assistant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    
    @EntityGraph(attributePaths = {"messages"})
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"messages"})
    Optional<ChatSession> findById(UUID id);

    @Query("SELECT s FROM ChatSession s WHERE s.expiresAt IS NOT NULL AND s.expiresAt <= :now")
    List<ChatSession> findExpiredSessions(@Param("now") Instant now);
}
