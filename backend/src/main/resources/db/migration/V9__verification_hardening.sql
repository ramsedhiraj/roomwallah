ALTER TABLE verification_requests
ADD COLUMN idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX idx_verification_idempotency
ON verification_requests(idempotency_key);

ALTER TABLE verification_requests
ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE fraud_signals
ADD COLUMN broker_risk_score INT DEFAULT 0;

ALTER TABLE trust_scores
ADD CONSTRAINT chk_trust_score
CHECK (overall_score BETWEEN 0 AND 100);

ALTER TABLE verification_requests
ADD CONSTRAINT chk_confidence
CHECK (
    confidence_score IS NULL
    OR (confidence_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_verification_provider_reference
ON verification_requests(provider_reference);

CREATE INDEX idx_verification_user_status
ON verification_requests(user_id, request_status);
