ALTER TABLE verification_requests
ADD COLUMN verification_version INT NOT NULL DEFAULT 1;

ALTER TABLE verification_requests
ADD COLUMN idempotency_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE verification_requests
ADD COLUMN idempotency_cleanup_after TIMESTAMP WITH TIME ZONE;

CREATE TABLE verification_provider_metrics (
    provider VARCHAR(50) PRIMARY KEY,
    total_requests BIGINT NOT NULL DEFAULT 0,
    successful_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    timeout_requests BIGINT NOT NULL DEFAULT 0,
    average_latency_ms BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_verification_idempotency_cleanup
ON verification_requests(idempotency_cleanup_after)
WHERE idempotency_key IS NOT NULL;
