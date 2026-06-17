package com.roomwallah.audit.repository;

import com.roomwallah.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByCorrelationId(String correlationId);
    List<AuditLog> findByOperator(String operator);
    List<AuditLog> findByAction(String action);
}
