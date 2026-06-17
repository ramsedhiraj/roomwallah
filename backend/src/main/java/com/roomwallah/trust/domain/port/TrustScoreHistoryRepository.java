package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.TrustScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrustScoreHistoryRepository extends JpaRepository<TrustScoreHistory, UUID> {
    List<TrustScoreHistory> findByUserIdOrderByCalculatedAtDesc(UUID userId);
}
