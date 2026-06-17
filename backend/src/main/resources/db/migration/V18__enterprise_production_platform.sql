-- 1. Analytics tables
CREATE TABLE hourly_metrics_snapshots (
    id UUID PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    snapshot_hour INT NOT NULL,
    total_bookings INT NOT NULL,
    active_listings INT NOT NULL,
    revenue DECIMAL(15, 2) NOT NULL,
    occupancy_rate DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_hourly_date_hour UNIQUE (snapshot_date, snapshot_hour)
);

CREATE TABLE daily_metrics_snapshots (
    id UUID PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    total_bookings INT NOT NULL,
    active_listings INT NOT NULL,
    revenue DECIMAL(15, 2) NOT NULL,
    occupancy_rate DECIMAL(5, 2) NOT NULL,
    rolling_7d_bookings INT NOT NULL DEFAULT 0,
    rolling_7d_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    rolling_30d_bookings INT NOT NULL DEFAULT 0,
    rolling_30d_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE geographic_analytics (
    id UUID PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    booking_count INT NOT NULL,
    revenue_generated DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_geo_date_city_country UNIQUE (snapshot_date, city, country)
);

CREATE INDEX idx_geo_date ON geographic_analytics(snapshot_date);

-- 2. User interactions log table
CREATE TABLE listing_interactions (
    id UUID PRIMARY KEY,
    user_id UUID,
    listing_id UUID NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    interaction_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_listing_interactions_property FOREIGN KEY (listing_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_interactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 3. Cryptographic Audit logs table (HMAC-SHA256 chain)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    operator VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100),
    target_entity_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    payload TEXT,
    error_message TEXT,
    correlation_id VARCHAR(100),
    current_hash VARCHAR(64) NOT NULL,
    previous_hash VARCHAR(64),
    chain_version INT NOT NULL DEFAULT 1,
    integrity_status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX idx_audit_logs_operator ON audit_logs(operator);

-- 4. Notification Center tables
CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_in_app_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notification_retry_queue (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_log TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

-- 5. Fraud Detection tables
CREATE TABLE fraud_rule_sets (
    id UUID PRIMARY KEY,
    version_name VARCHAR(100) NOT NULL UNIQUE,
    velocity_limit INT NOT NULL DEFAULT 3,
    large_transaction_limit DECIMAL(15, 2) NOT NULL DEFAULT 100000.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE fraud_events (
    id UUID PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(100) NOT NULL,
    details TEXT,
    risk_score DECIMAL(5, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_fraud_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE fraud_cases (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    risk_score DECIMAL(5, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    rule_set_version VARCHAR(100),
    reviewer_id UUID,
    reviewer_notes TEXT,
    escalated_to VARCHAR(100),
    escalated_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_fraud_cases_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Partner APIs tables
CREATE TABLE partner_api_keys (
    id UUID PRIMARY KEY,
    partner_name VARCHAR(100) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    scopes VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE,
    daily_quota_limit INT NOT NULL DEFAULT 1000,
    current_daily_usage INT NOT NULL DEFAULT 0,
    quota_reset_at TIMESTAMP WITH TIME ZONE NOT NULL,
    rotation_history TEXT,
    last_used_at TIMESTAMP WITH TIME ZONE,
    rotation_reminded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

-- 7. Configurable recommendations
CREATE TABLE recommendation_weights (
    id UUID PRIMARY KEY,
    budget_weight DOUBLE PRECISION NOT NULL DEFAULT 0.3,
    proximity_weight DOUBLE PRECISION NOT NULL DEFAULT 0.4,
    recency_weight DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    popularity_weight DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
