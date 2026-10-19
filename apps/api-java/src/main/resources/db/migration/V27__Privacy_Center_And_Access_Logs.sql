ALTER TABLE users
    ADD COLUMN activity_visibility VARCHAR(20) NOT NULL DEFAULT 'COMMUNITY';

ALTER TABLE communities
    ADD COLUMN open_data_policy VARCHAR(40) NOT NULL DEFAULT 'DISABLED';

ALTER TABLE communities
    ADD COLUMN privacy_updated_by UUID NULL;

ALTER TABLE communities
    ADD COLUMN privacy_updated_at TIMESTAMP NULL;

CREATE TABLE sensitive_data_access_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    community_id UUID NULL REFERENCES communities(id) ON DELETE SET NULL,
    access_type VARCHAR(40) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensitive_access_target_created
    ON sensitive_data_access_logs (target_user_id, created_at DESC);

CREATE INDEX idx_sensitive_access_actor_created
    ON sensitive_data_access_logs (actor_user_id, created_at DESC);

CREATE INDEX idx_sensitive_access_community_created
    ON sensitive_data_access_logs (community_id, created_at DESC);
