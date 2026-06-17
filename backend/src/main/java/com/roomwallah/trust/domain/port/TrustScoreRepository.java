package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository("trustContextTrustScoreRepository")
public interface TrustScoreRepository extends JpaRepository<TrustScore, UUID> {
    Optional<TrustScore> findByUserId(UUID userId);
}
