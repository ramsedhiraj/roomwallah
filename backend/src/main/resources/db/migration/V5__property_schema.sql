CREATE TABLE properties (
    id UUID PRIMARY KEY,
    listing_ref VARCHAR(100) UNIQUE NOT NULL,
    owner_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    property_type VARCHAR(50) NOT NULL,
    listing_purpose VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    
    -- Visibility
    visibility VARCHAR(50) DEFAULT 'PUBLIC' NOT NULL,
    
    -- Monetary fields using DECIMAL(15, 2) with CHECK constraints
    price_amount DECIMAL(15, 2) NOT NULL,
    price_currency VARCHAR(10) DEFAULT 'INR' NOT NULL,
    security_deposit_amount DECIMAL(15, 2),
    security_deposit_currency VARCHAR(10) DEFAULT 'INR',
    maintenance_charges_amount DECIMAL(15, 2),
    maintenance_charges_currency VARCHAR(10) DEFAULT 'INR',
    negotiable BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Embedded Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    
    -- Embedded GeoLocation using DECIMAL with CHECK constraints
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Embedded AreaMeasurement using DECIMAL(15, 4) with CHECK constraints
    area_value DECIMAL(15, 4) NOT NULL,
    area_unit VARCHAR(20) NOT NULL,
    
    bedrooms INT,
    bathrooms INT,
    parking_count INT DEFAULT 0 NOT NULL,
    parking_type VARCHAR(50),
    furnishing_status VARCHAR(50),
    
    -- Enriched Optional Metadata
    construction_year INT,
    floor_number INT,
    total_floors INT,
    facing_direction VARCHAR(50),
    possession_status VARCHAR(50),
    pet_friendly BOOLEAN DEFAULT FALSE NOT NULL,
    
    availability_date DATE,
    
    -- Lifecycle Timestamps
    published_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    
    -- Moderation Metadata
    moderation_status VARCHAR(50),
    moderation_reason TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    
    -- SEO friendly slug
    slug VARCHAR(255) UNIQUE,
    
    -- Audits and Locking
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_properties_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    
    -- Integrity Check Constraints
    CONSTRAINT chk_properties_price_amount CHECK (price_amount >= 0),
    CONSTRAINT chk_properties_security_deposit CHECK (security_deposit_amount >= 0),
    CONSTRAINT chk_properties_maintenance_charges CHECK (maintenance_charges_amount >= 0),
    CONSTRAINT chk_properties_latitude CHECK (latitude BETWEEN -90.0 AND 90.0),
    CONSTRAINT chk_properties_longitude CHECK (longitude BETWEEN -180.0 AND 180.0),
    CONSTRAINT chk_properties_area_value CHECK (area_value > 0.0)
);

-- Separate table for extensible amenities
CREATE TABLE property_amenities (
    property_id UUID NOT NULL,
    amenity VARCHAR(100) NOT NULL,
    PRIMARY KEY (property_id, amenity),
    CONSTRAINT fk_property_amenities_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- Search and filter indexes
CREATE INDEX idx_properties_owner_id ON properties(owner_id);
CREATE INDEX idx_properties_listing_ref ON properties(listing_ref);
CREATE INDEX idx_properties_type ON properties(property_type);
CREATE INDEX idx_properties_purpose ON properties(listing_purpose);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_visibility ON properties(visibility);
CREATE INDEX idx_properties_created_at ON properties(created_at);
