-- Migration V26: Repair dev user preferences
-- Populates default user preferences for any user that exists in the database but lacks a preferences record.

INSERT INTO user_preferences (
    id, 
    user_id, 
    dark_mode_preferred, 
    email_notifications_enabled, 
    push_notifications_enabled, 
    marketing_notifications_enabled, 
    preferred_language, 
    preferred_contact_method, 
    created_at, 
    updated_at, 
    version
)
SELECT 
    gen_random_uuid(), 
    id, 
    FALSE, 
    TRUE, 
    TRUE, 
    FALSE, 
    'en', 
    'EMAIL', 
    NOW(), 
    NOW(), 
    0
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_preferences p WHERE p.user_id = u.id
);
