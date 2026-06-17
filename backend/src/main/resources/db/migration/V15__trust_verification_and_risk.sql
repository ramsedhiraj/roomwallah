-- Flyway Migration: Trust Verification and Risk

-- Alter trust_scores table to align with trust domain needs
ALTER TABLE trust_scores ADD COLUMN current_score INT;
ALTER TABLE trust_scores ADD COLUMN score_version INT;
ALTER TABLE trust_scores ADD COLUMN rule_version VARCHAR(50);
ALTER TABLE trust_scores ADD COLUMN algorithm_version VARCHAR(50);
ALTER TABLE trust_scores ADD COLUMN explanation_json TEXT;
ALTER TABLE trust_scores ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE trust_scores ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Backfill data from older schema to new schema columns
UPDATE trust_scores SET current_score = overall_score WHERE current_score IS NULL;
UPDATE trust_scores SET score_version = 1 WHERE score_version IS NULL;
UPDATE trust_scores SET rule_version = 'v1.0' WHERE rule_version IS NULL;
UPDATE trust_scores SET algorithm_version = 'v1.0' WHERE algorithm_version IS NULL;
UPDATE trust_scores SET explanation_json = '{}' WHERE explanation_json IS NULL;
UPDATE trust_scores SET created_at = calculated_at WHERE created_at IS NULL;
UPDATE trust_scores SET updated_at = calculated_at WHERE updated_at IS NULL;

-- Set altered fields as NOT NULL
ALTER TABLE trust_scores ALTER COLUMN current_score SET NOT NULL;
ALTER TABLE trust_scores ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE trust_scores ALTER COLUMN updated_at SET NOT NULL;

-- Alter fraud_signals table to align with trust domain needs
ALTER TABLE fraud_signals ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE fraud_signals ADD COLUMN version BIGINT;
ALTER TABLE fraud_signals ADD COLUMN fraud_type VARCHAR(100);
ALTER TABLE fraud_signals ADD COLUMN metadata_json TEXT;
ALTER TABLE fraud_signals ADD COLUMN detected_at TIMESTAMP WITH TIME ZONE;

-- Backfill older data
UPDATE fraud_signals SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE fraud_signals SET version = 1 WHERE version IS NULL;
UPDATE fraud_signals SET fraud_type = signal_type WHERE fraud_type IS NULL;
UPDATE fraud_signals SET metadata_json = '{}' WHERE metadata_json IS NULL;
UPDATE fraud_signals SET detected_at = created_at WHERE detected_at IS NULL;

-- Set altered fields as NOT NULL
ALTER TABLE fraud_signals ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE fraud_signals ALTER COLUMN version SET NOT NULL;
ALTER TABLE fraud_signals ALTER COLUMN fraud_type SET NOT NULL;
ALTER TABLE fraud_signals ALTER COLUMN detected_at SET NOT NULL;

-- Create owner_verifications table
CREATE TABLE owner_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    verification_status VARCHAR(50) NOT NULL,
    verification_level VARCHAR(50) NOT NULL,
    verification_provider VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    reviewer_id UUID,
    rejection_reason VARCHAR(255),
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_owner_verifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX idx_owner_verifications_idempotency ON owner_verifications(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_owner_verifications_user ON owner_verifications(user_id);
CREATE INDEX idx_owner_verifications_status ON owner_verifications(verification_status);

-- Create verification_documents table
CREATE TABLE verification_documents (
    id UUID PRIMARY KEY,
    verification_id UUID NOT NULL,
    media_id UUID NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    encrypted_metadata TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_verification_documents_verification FOREIGN KEY (verification_id) REFERENCES owner_verifications(id) ON DELETE CASCADE
);

CREATE INDEX idx_verification_documents_verification ON verification_documents(verification_id);

-- Create trust_score_history table
CREATE TABLE trust_score_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    previous_score INT NOT NULL,
    new_score INT NOT NULL,
    reason TEXT,
    triggered_by_event VARCHAR(255),
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_trust_score_history_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_trust_score_history_user ON trust_score_history(user_id);

-- Create broker_detection_signals table
CREATE TABLE broker_detection_signals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    signal_type VARCHAR(100) NOT NULL,
    signal_weight DECIMAL(10, 4) NOT NULL,
    metadata_json TEXT,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_broker_detection_signals_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_broker_detection_signals_user ON broker_detection_signals(user_id);

-- Create moderation_cases table
CREATE TABLE moderation_cases (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    assigned_admin UUID,
    priority_score DECIMAL(10, 4) NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_moderation_cases_entity ON moderation_cases(entity_type, entity_id);
CREATE INDEX idx_moderation_cases_status ON moderation_cases(status);

-- Create trust_shedlock table
CREATE TABLE trust_shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    locked_by VARCHAR(255)
);
