package com.roomwallah.media.domain.repository;

import com.roomwallah.media.domain.entity.UploadIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface UploadIdempotencyRepository extends JpaRepository<UploadIdempotency, String> {
    @Transactional
    @Modifying
    @Query("DELETE FROM UploadIdempotency u WHERE u.createdAt < :cutoff")
    void deleteOlderThan(Instant cutoff);
}
