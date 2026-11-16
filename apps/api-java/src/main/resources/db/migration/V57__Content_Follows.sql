-- V57: follow a report, a conversation or an announcement. One row per person and target.

CREATE TABLE content_follows (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_content_follows_user_target UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_content_follows_target
    ON content_follows (target_type, target_id);
