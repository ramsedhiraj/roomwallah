-- Flyway Migration: Ensure core relations exist
-- This migration fixes issues where trust_shedlock, outbox_events, or domain_outbox_events might be missing.

-- 1. Ensure trust_shedlock table exists
CREATE TABLE IF NOT EXISTS trust_shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    locked_by VARCHAR(255)
);

-- 2. Ensure outbox_events table exists
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- 3. Ensure index for outbox_events status exists
-- Use DO block for idempotent index creation in Postgres
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 'idx_outbox_events_status' AND n.nspname = 'public') THEN
        CREATE INDEX idx_outbox_events_status ON outbox_events(status);
    END IF;
END $$;

-- 4. Ensure domain_outbox_events table exists
CREATE TABLE IF NOT EXISTS domain_outbox_events (
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
