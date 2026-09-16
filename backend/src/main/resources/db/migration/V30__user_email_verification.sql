-- Add email verification token hash and expiration columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token_expires_at TIMESTAMP WITH TIME ZONE;

-- Create index for quick token lookup
CREATE INDEX IF NOT EXISTS idx_users_email_verification_token_hash 
    ON users(email_verification_token_hash) 
    WHERE email_verification_token_hash IS NOT NULL;
