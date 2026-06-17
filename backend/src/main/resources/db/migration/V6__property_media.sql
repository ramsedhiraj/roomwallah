CREATE TABLE property_media (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    processing_status VARCHAR(50) NOT NULL,
    moderation_status VARCHAR(50) NOT NULL,
    display_order BIGINT NOT NULL,
    is_cover BOOLEAN DEFAULT FALSE NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    width INT,
    height INT,
    duration_seconds INT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    
    CONSTRAINT fk_property_media_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE INDEX idx_property_media_property_id ON property_media(property_id);
CREATE INDEX idx_property_media_type ON property_media(media_type);
CREATE INDEX idx_property_media_processing ON property_media(processing_status);
CREATE INDEX idx_property_media_moderation ON property_media(moderation_status);
CREATE INDEX idx_property_media_order ON property_media(display_order);
CREATE INDEX idx_property_media_cover ON property_media(is_cover);
