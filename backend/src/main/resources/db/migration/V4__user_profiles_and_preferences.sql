-- Extend users table with profile fields
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN avatar_key VARCHAR(255);
ALTER TABLE users ADD COLUMN date_of_birth DATE;
ALTER TABLE users ADD COLUMN gender VARCHAR(50);

-- Add verification flags
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN identity_verified BOOLEAN DEFAULT FALSE NOT NULL;

-- Create user_preferences table
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    dark_mode_preferred BOOLEAN DEFAULT FALSE NOT NULL,
    email_notifications_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    push_notifications_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    marketing_notifications_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    preferred_language VARCHAR(50) DEFAULT 'en' NOT NULL,
    preferred_contact_method VARCHAR(50) DEFAULT 'EMAIL' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
