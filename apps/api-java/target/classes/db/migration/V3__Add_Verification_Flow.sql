-- V3__Add_Verification_Flow.sql
-- Add support for email verification codes

ALTER TABLE users ADD COLUMN verification_code VARCHAR(6);
ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE;

-- Ensure users are disabled until verified
UPDATE users SET is_verified = TRUE, enabled = TRUE; -- existing seed users remain active
