package com.roomwallah.audit.domain;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String operator;

    @Column(name = "target_entity")
    private String targetEntity;

    @Column(name = "target_entity_id")
    private String targetEntityId;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "chain_version", nullable = false)
    private int chainVersion = 1;

    @Column(name = "integrity_status", nullable = false, length = 50)
    private String integrityStatus = "VALID";
}
