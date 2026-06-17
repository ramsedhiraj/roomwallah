CREATE TABLE search_documents (
    property_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    listing_ref VARCHAR(100) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    country VARCHAR(100),
    property_type VARCHAR(50) NOT NULL,
    listing_purpose VARCHAR(50) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    bedrooms INT,
    bathrooms INT,
    parking_count INT NOT NULL DEFAULT 0,
    furnishing_status VARCHAR(50),
    pet_friendly BOOLEAN NOT NULL DEFAULT FALSE,
    trust_score INT NOT NULL DEFAULT 0,
    owner_verified BOOLEAN NOT NULL DEFAULT FALSE,
    owner_badge VARCHAR(50),
    property_status VARCHAR(50) NOT NULL,
    media_count INT NOT NULL DEFAULT 0,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_version BIGINT NOT NULL DEFAULT 1,
    last_event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE saved_searches (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    serialized_query TEXT NOT NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_saved_searches_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE search_analytics (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL,
    user_id UUID,
    search_text VARCHAR(255),
    filters_json TEXT,
    execution_time_ms BIGINT NOT NULL,
    result_count INT NOT NULL,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE trending_queries (
    id UUID PRIMARY KEY,
    query_text VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    search_count INT NOT NULL,
    last_aggregated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_search_documents_city ON search_documents(city);
CREATE INDEX idx_search_documents_price ON search_documents(price);
CREATE INDEX idx_search_documents_status ON search_documents(property_status);
CREATE INDEX idx_search_documents_trust ON search_documents(trust_score);
CREATE INDEX idx_search_documents_coords ON search_documents(latitude, longitude);

CREATE INDEX idx_saved_searches_user ON saved_searches(user_id);
CREATE INDEX idx_saved_searches_created ON saved_searches(created_at);

CREATE INDEX idx_search_analytics_text ON search_analytics(search_text);
CREATE INDEX idx_search_analytics_created ON search_analytics(created_at);
CREATE INDEX idx_trending_queries_city ON trending_queries(city);
