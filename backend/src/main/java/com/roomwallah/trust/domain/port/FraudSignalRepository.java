package com.roomwallah.trust.domain.port;

import com.roomwallah.trust.domain.entity.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository("trustContextFraudSignalRepository")
public interface FraudSignalRepository extends JpaRepository<FraudSignal, UUID> {
    List<FraudSignal> findByUserId(UUID userId);
}
