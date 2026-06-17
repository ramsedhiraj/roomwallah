-- Migration V24: Add missing version columns to aadhaar_verifications and user_verification_logs
-- These tables extend BaseEntity which includes an @Version optimistic locking column.

ALTER TABLE aadhaar_verifications ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_verification_logs ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
