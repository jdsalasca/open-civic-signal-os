ALTER TABLE signals
    ADD COLUMN IF NOT EXISTS assigned_to_user_id UUID;

ALTER TABLE signal_status_history
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50) DEFAULT 'STATUS_CHANGED' NOT NULL;

ALTER TABLE signal_status_history
    ADD COLUMN IF NOT EXISTS assigned_to_username VARCHAR(255);
