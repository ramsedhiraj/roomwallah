package com.roomwallah.media.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "media_derivatives")
@Getter
@Setter
public class MediaDerivative extends BaseEntity {

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "variant_type", nullable = false, length = 50)
    private String variantType;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;
}
