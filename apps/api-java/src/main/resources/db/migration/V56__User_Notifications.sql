-- V56: personal notification inbox. The old notifications table was a staff relay log;
-- this one is scoped to a recipient and carries enough structure to localize the message on the
-- client (type + actor + target) instead of storing English prose.

CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    actor_id UUID,
    actor_name VARCHAR(80),
    type VARCHAR(40) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    target_title VARCHAR(255),
    community_id UUID,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE INDEX idx_user_notifications_user_created
    ON user_notifications (user_id, created_at);

CREATE INDEX idx_user_notifications_user_read
    ON user_notifications (user_id, read_at);
