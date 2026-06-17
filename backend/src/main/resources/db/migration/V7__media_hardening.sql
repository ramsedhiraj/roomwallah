-- Phase 4 Hardening Migration

-- 1. Extend property_media table
ALTER TABLE property_media ADD COLUMN revision INT DEFAULT 1 NOT NULL;
ALTER TABLE property_media ADD COLUMN uploaded_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN scanned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN optimized_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN moderated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN thumbnail_generated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN ready_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE property_media ADD COLUMN storage_provider VARCHAR(50);

-- 2. Create upload_idempotency table
CREATE TABLE upload_idempotency (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    media_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 3. Create media_derivatives table
CREATE TABLE media_derivatives (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL,
    variant_type VARCHAR(50) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    width INT,
    height INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_media_derivatives_media FOREIGN KEY (media_id) REFERENCES property_media(id),
    CONSTRAINT uq_media_derivatives_variant UNIQUE (media_id, variant_type)
);

CREATE INDEX idx_media_derivatives_media_id ON media_derivatives(media_id);
CREATE INDEX idx_media_derivatives_variant ON media_derivatives(variant_type);
