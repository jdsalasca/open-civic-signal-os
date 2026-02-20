-- V6__Add_ScoreBreakdown_Columns.sql
-- Align embedded ScoreBreakdown columns with Signal entity mapping.

ALTER TABLE signals ADD COLUMN IF NOT EXISTS score_urgency DOUBLE PRECISION;
ALTER TABLE signals ADD COLUMN IF NOT EXISTS score_impact DOUBLE PRECISION;
ALTER TABLE signals ADD COLUMN IF NOT EXISTS score_affected_people DOUBLE PRECISION;
ALTER TABLE signals ADD COLUMN IF NOT EXISTS score_community_votes DOUBLE PRECISION;
