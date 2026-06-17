package com.roomwallah.identity.infrastructure.repository;

import com.roomwallah.identity.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    List<UserSession> findByUserIdAndRevokedFalse(UUID userId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId AND s.revoked = false")
    void revokeAllByUserId(UUID userId);
}
