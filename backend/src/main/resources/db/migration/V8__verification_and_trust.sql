CREATE TABLE verification_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    request_status VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(255),
    verified_name VARCHAR(255),
    confidence_score DECIMAL(5, 2),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_verification_requests_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE verification_audit (
    id UUID PRIMARY KEY,
    verification_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor UUID,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata TEXT,
    CONSTRAINT fk_verification_audit_request FOREIGN KEY (verification_id) REFERENCES verification_requests(id)
);

CREATE TABLE trust_scores (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    overall_score INT NOT NULL,
    identity_score INT NOT NULL,
    property_score INT NOT NULL,
    review_score INT NOT NULL,
    activity_score INT NOT NULL,
    fraud_penalty INT NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_trust_scores_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE fraud_signals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    signal_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fraud_signals_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE verification_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    badge_level VARCHAR(50) NOT NULL,
    awarded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT fk_verification_badges_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_verification_requests_user_id ON verification_requests(user_id);
CREATE INDEX idx_verification_requests_status ON verification_requests(request_status);
CREATE INDEX idx_verification_audit_request_id ON verification_audit(verification_id);
CREATE INDEX idx_trust_scores_user_id ON trust_scores(user_id);
CREATE INDEX idx_fraud_signals_user_id ON fraud_signals(user_id);
CREATE INDEX idx_verification_badges_user_id ON verification_badges(user_id);
CREATE INDEX idx_outbox_events_status ON outbox_events(status);
