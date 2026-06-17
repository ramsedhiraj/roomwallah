CREATE TABLE ai_feedback (
    id UUID PRIMARY KEY,
    target_type VARCHAR(50) NOT NULL, -- CHAT_MESSAGE, RECOMMENDATION, SEARCH
    target_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_positive BOOLEAN NOT NULL,
    issue_report TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

ALTER TABLE chat_messages ADD COLUMN model_version VARCHAR(50);
ALTER TABLE chat_messages ADD COLUMN prompt_template_version VARCHAR(50);

ALTER TABLE search_intent_logs ADD COLUMN model_version VARCHAR(50);
ALTER TABLE search_intent_logs ADD COLUMN prompt_template_version VARCHAR(50);

CREATE TABLE tenant_ai_quotas (
    tenant_id VARCHAR(100) PRIMARY KEY,
    monthly_limit_usd DECIMAL(15, 4) NOT NULL,
    current_spend_usd DECIMAL(15, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

-- Prepopulate some default quotas
INSERT INTO tenant_ai_quotas (tenant_id, monthly_limit_usd, current_spend_usd, created_at, updated_at, version)
VALUES ('default', 500.0000, 0.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       ('t1', 10.0000, 0.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0); -- Low quota for testing
