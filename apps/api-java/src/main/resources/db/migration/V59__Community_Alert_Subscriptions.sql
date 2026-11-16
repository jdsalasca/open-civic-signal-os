-- V59: opt-in alerts for one community's new reports and events.

CREATE TABLE community_alert_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    community_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_community_alert_user UNIQUE (user_id, community_id)
);

CREATE INDEX idx_community_alert_community
    ON community_alert_subscriptions (community_id);
