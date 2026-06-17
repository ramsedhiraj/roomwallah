-- AI Semantic search tables
CREATE TABLE search_synonyms (
    id UUID PRIMARY KEY,
    term VARCHAR(100) NOT NULL UNIQUE,
    synonyms TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE search_intent_logs (
    id UUID PRIMARY KEY,
    query_text VARCHAR(255) NOT NULL,
    parsed_intent TEXT,
    confidence DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

-- Vector Recommendations tables
CREATE TABLE listing_vectors (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    embedding double precision[] NOT NULL,
    embedding_version INT NOT NULL,
    model_identifier VARCHAR(100) NOT NULL,
    generation_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_listing_vectors_property FOREIGN KEY (listing_id) REFERENCES properties(id) ON DELETE CASCADE
);

CREATE TABLE user_vector_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    preferred_embedding double precision[] NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_user_vector_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE recommendation_clicks (
    id UUID PRIMARY KEY,
    user_id UUID,
    listing_id UUID NOT NULL,
    algorithm_version VARCHAR(50) NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_rec_clicks_property FOREIGN KEY (listing_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- Conversational property assistant tables
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255),
    summary TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

-- Domain event outbox table
CREATE TABLE domain_outbox_events (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_log TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

-- Localization translations
CREATE TABLE localized_translations (
    id UUID PRIMARY KEY,
    translation_key VARCHAR(255) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    translation_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_translation_key_locale UNIQUE (translation_key, locale)
);

-- Prepopulate translations
INSERT INTO localized_translations (id, translation_key, locale, translation_value, created_at, updated_at, version) VALUES
('a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'listing.not_found', 'en-IN', 'Listing not found', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b2c3d4e5-f67a-8b9c-0d1e-2f3a4b5c6d7e', 'listing.not_found', 'hi-IN', 'लिस्टिंग नहीं मिली', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f', 'listing.not_found', 'mr-IN', 'लिस्टिंग सापडली नाही', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
