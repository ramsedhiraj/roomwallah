CREATE TABLE suspected_duplicate_clusters (
    id VARCHAR(100) PRIMARY KEY,
    similarity_score DOUBLE PRECISION NOT NULL,
    locality VARCHAR(255),
    city VARCHAR(255),
    candidate_a_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    candidate_b_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    match_insights TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE high_risk_approval_requests (
    id UUID PRIMARY KEY,
    action_type VARCHAR(100) NOT NULL, -- REJECT_LISTING, SUSPEND_USER, BLOCK_OWNER, MODIFY_PRICE
    target_id VARCHAR(100) NOT NULL,
    requested_by VARCHAR(100) NOT NULL, -- SYSTEM_AI, USER_REPORT
    reason TEXT NOT NULL,
    proposed_data TEXT,
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(100),
    version BIGINT NOT NULL
);
