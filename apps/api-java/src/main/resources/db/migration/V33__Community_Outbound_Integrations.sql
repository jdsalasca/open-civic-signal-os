CREATE TABLE community_integrations (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    channel VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    target_uri TEXT NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP NULL,
    last_success_at TIMESTAMP NULL,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    auto_retry BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_community_integrations_community_channel
    ON community_integrations (community_id, channel, enabled);

CREATE TABLE community_integration_deliveries (
    id UUID PRIMARY KEY,
    integration_id UUID NOT NULL REFERENCES community_integrations(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    reference_id UUID NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(280) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
);

CREATE INDEX idx_community_integration_deliveries_integration_status
    ON community_integration_deliveries (integration_id, status, created_at DESC);

CREATE INDEX idx_community_integration_deliveries_pending
    ON community_integration_deliveries (status, created_at);