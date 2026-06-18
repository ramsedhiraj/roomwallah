-- Create necessary tables for the application
CREATE TABLE IF NOT EXISTS trust_shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS booking_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS domain_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    error_log TEXT,
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0,
    next_attempt_at TIMESTAMP,
    idempotency_key VARCHAR(255)
);
