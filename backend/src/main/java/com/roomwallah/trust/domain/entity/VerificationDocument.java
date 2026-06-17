package com.roomwallah.trust.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDocument extends BaseEntity {

    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "encrypted_metadata", columnDefinition = "TEXT")
    private String encryptedMetadata;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
