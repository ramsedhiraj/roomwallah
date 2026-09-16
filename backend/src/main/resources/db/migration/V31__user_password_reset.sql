-- Add password reset token hash and expiration columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMP WITH TIME ZONE;

-- Create partial index for rapid lookup
CREATE INDEX IF NOT EXISTS idx_users_password_reset_token_hash 
    ON users(password_reset_token_hash) 
    WHERE password_reset_token_hash IS NOT NULL;
