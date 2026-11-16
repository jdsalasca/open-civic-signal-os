-- V60: one reminder per activity, sent roughly 24 hours before it starts.

ALTER TABLE community_activities
    ADD COLUMN reminder_sent_at TIMESTAMP;
