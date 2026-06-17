CREATE TABLE lease_agreements (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agreement_content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING_SIGNATURE, SIGNED, EXPIRED, TERMINATED
    rent_amount DECIMAL(15, 4) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    terms_version INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE agreement_signatures (
    id UUID PRIMARY KEY,
    agreement_id UUID NOT NULL REFERENCES lease_agreements(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    signature_hash VARCHAR(256) NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(100) NOT NULL
);

CREATE TABLE secure_documents (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- LEASE, KYC, OWNERSHIP_PROOF, UTILITY_BILL, INVOICE
    file_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    is_encrypted BOOLEAN NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE document_access_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES secure_documents(id) ON DELETE CASCADE,
    accessor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_type VARCHAR(50) NOT NULL, -- READ, WRITE, DELETE, DOWNLOAD
    accessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address VARCHAR(45) NOT NULL
);

CREATE TABLE remediation_tasks (
    id UUID PRIMARY KEY,
    target_type VARCHAR(50) NOT NULL, -- PROPERTY, USER
    target_id VARCHAR(100) NOT NULL,
    issue_type VARCHAR(100) NOT NULL, -- STALE_LISTING, COORDINATE_MISMATCH, BROKEN_IMAGE, INVALID_ADDRESS
    status VARCHAR(50) NOT NULL, -- PENDING, RESOLVED, IGNORED
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE experiment_cohorts (
    id UUID PRIMARY KEY,
    experiment_name VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    cohort VARCHAR(50) NOT NULL, -- CONTROL, TREATMENT
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ml_features (
    feature_key VARCHAR(100) PRIMARY KEY,
    feature_value TEXT NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    feature_version INT NOT NULL
);

CREATE TABLE search_evaluations (
    id UUID PRIMARY KEY,
    eval_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    ndcg DECIMAL(5, 4) NOT NULL,
    precision_k DECIMAL(5, 4) NOT NULL,
    recall_k DECIMAL(5, 4) NOT NULL,
    ctr DECIMAL(5, 4) NOT NULL,
    abandonment_rate DECIMAL(5, 4) NOT NULL,
    run_details TEXT
);

CREATE TABLE active_plugins (
    id VARCHAR(100) PRIMARY KEY,
    plugin_name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, INACTIVE, ERROR
    permissions TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
