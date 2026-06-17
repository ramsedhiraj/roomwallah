-- Migration V23: User verification history, Aadhaar verification, and property verification schemas

-- 1. Alter users table to add email and phone verification timestamps
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN phone_verified_at TIMESTAMP WITH TIME ZONE;

-- 2. Create user verification logs for tracking OTP requests and validations
CREATE TABLE user_verification_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    verification_type VARCHAR(50) NOT NULL, -- EMAIL_OTP, MOBILE_OTP
    status VARCHAR(50) NOT NULL, -- PENDING, VERIFIED, EXPIRED, FAILED
    attempts INT NOT NULL DEFAULT 0,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_verification_logs_user_id ON user_verification_logs(user_id);

-- 3. Create aadhaar verifications for tracking identity status and consent
CREATE TABLE aadhaar_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_aadhaar TEXT NOT NULL,
    masked_aadhaar VARCHAR(20) NOT NULL,
    consent_tracked BOOLEAN NOT NULL DEFAULT TRUE,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_aadhaar_verifications_user_id ON aadhaar_verifications(user_id);

-- 4. Create property verifications to store deeds, utilities, location checks, and confidence score
CREATE TABLE property_verifications (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_url VARCHAR(512) NOT NULL,
    utility_bill_url VARCHAR(512) NOT NULL,
    deed_name_matched BOOLEAN NOT NULL DEFAULT FALSE,
    utility_name_matched BOOLEAN NOT NULL DEFAULT FALSE,
    location_matched BOOLEAN NOT NULL DEFAULT FALSE,
    confidence_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    approval_status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED
    rejection_reason TEXT,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_property_verifications_property_id ON property_verifications(property_id);
CREATE INDEX idx_property_verifications_owner_id ON property_verifications(owner_id);
