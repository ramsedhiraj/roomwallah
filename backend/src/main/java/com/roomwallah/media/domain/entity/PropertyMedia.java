package com.roomwallah.media.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import com.roomwallah.media.domain.valueobject.FileChecksum;
import com.roomwallah.media.domain.valueobject.MediaMetadata;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "property_media")
@Getter
@Setter
public class PropertyMedia extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 50)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 50)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "display_order", nullable = false)
    private long displayOrder;

    @Column(name = "is_cover", nullable = false)
    private boolean isCover = false;

    @Embedded
    private MediaMetadata metadata;

    @Embedded
    private FileChecksum checksum;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Hardening extensions
    @Column(name = "revision", nullable = false)
    private int revision = 1;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @Column(name = "optimized_at")
    private Instant optimizedAt;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "thumbnail_generated_at")
    private Instant thumbnailGeneratedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "storage_provider", length = 50)
    private String storageProvider;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "media_id")
    private List<MediaDerivative> derivatives = new ArrayList<>();

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }
}
