package com.roomwallah.common.observability;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAiQuotaRepository extends JpaRepository<TenantAiQuota, String> {
}
