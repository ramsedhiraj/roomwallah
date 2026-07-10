package com.roomwallah.chat.domain.repository;

import com.roomwallah.chat.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByBookingId(UUID bookingId);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :userId OR c.ownerId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c WHERE (c.tenantId = :u1 AND c.ownerId = :u2) OR (c.tenantId = :u2 AND c.ownerId = :u1)")
    List<Conversation> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
