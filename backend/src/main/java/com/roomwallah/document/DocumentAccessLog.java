package com.roomwallah.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "accessor_id", nullable = false)
    private UUID accessorId;

    @Column(name = "access_type", nullable = false, length = 50)
    private String accessType; // READ, WRITE, DELETE, DOWNLOAD

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
}
