-- Drop unique constraint on phone if exists
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_phone;

-- Make phone and password_hash nullable
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Create partial unique index on phone (ignore null values, only unique when non-null)
CREATE UNIQUE INDEX uq_users_phone_partial ON users(phone) WHERE phone IS NOT NULL AND deleted = FALSE;

-- Add provider and provider_id columns for OAuth support
ALTER TABLE users ADD COLUMN provider VARCHAR(50) DEFAULT 'LOCAL' NOT NULL;
ALTER TABLE users ADD COLUMN provider_id VARCHAR(100);

-- Add lock_until column for account lockouts
ALTER TABLE users ADD COLUMN lock_until TIMESTAMP WITH TIME ZONE;

-- Create indexes
CREATE INDEX idx_users_provider_provider_id ON users(provider, provider_id);
