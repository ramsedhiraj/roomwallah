CREATE TABLE verification_decision_audit (
    id UUID PRIMARY KEY,
    verification_request_id UUID NOT NULL,
    admin_id UUID NOT NULL,
    previous_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    decision_reason TEXT NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_verification_decision_request
        FOREIGN KEY (verification_request_id)
        REFERENCES verification_requests(id)
);

CREATE INDEX idx_decision_request ON verification_decision_audit(verification_request_id);
CREATE INDEX idx_decision_admin ON verification_decision_audit(admin_id);
CREATE INDEX idx_decision_created ON verification_decision_audit(created_at);
CREATE INDEX idx_decision_correlation ON verification_decision_audit(correlation_id);
