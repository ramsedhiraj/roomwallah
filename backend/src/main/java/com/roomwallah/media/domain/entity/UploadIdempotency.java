package com.roomwallah.media.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_idempotency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadIdempotency {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
