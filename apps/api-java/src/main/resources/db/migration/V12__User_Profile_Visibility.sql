-- V12__User_Profile_Visibility.sql
-- Enriched civic identity profile fields and deterministic visibility controls

ALTER TABLE users ADD COLUMN display_name VARCHAR(80);
ALTER TABLE users ADD COLUMN civic_role VARCHAR(40);
ALTER TABLE users ADD COLUMN affiliations_csv TEXT;
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN profile_visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE users ADD COLUMN affiliation_visibility VARCHAR(32) NOT NULL DEFAULT 'COMMUNITY';
