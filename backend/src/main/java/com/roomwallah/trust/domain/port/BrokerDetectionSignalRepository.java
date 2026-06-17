package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.BrokerDetectionSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BrokerDetectionSignalRepository extends JpaRepository<BrokerDetectionSignal, UUID> {
    List<BrokerDetectionSignal> findByUserId(UUID userId);
}
