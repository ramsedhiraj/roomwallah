package com.roomwallah.verification.domain.repository;

import com.roomwallah.verification.domain.entity.VerificationProviderMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationProviderMetricsRepository extends JpaRepository<VerificationProviderMetrics, String> {
}
