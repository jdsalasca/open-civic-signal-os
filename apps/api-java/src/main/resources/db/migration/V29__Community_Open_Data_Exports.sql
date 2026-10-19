CREATE TABLE community_open_data_tokens (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    label VARCHAR(120) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_prefix VARCHAR(36) NOT NULL,
    scopes_csv TEXT NOT NULL,
    rate_limit_per_hour INTEGER NOT NULL DEFAULT 120,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL
);

CREATE INDEX idx_open_data_tokens_community_created
    ON community_open_data_tokens (community_id, created_at DESC);

CREATE TABLE community_open_data_access_logs (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    actor_user_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    token_id UUID NULL REFERENCES community_open_data_tokens(id) ON DELETE SET NULL,
    access_channel VARCHAR(30) NOT NULL,
    export_type VARCHAR(30) NOT NULL,
    format VARCHAR(20) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_open_data_access_logs_community_created
    ON community_open_data_access_logs (community_id, created_at DESC);

CREATE INDEX idx_open_data_access_logs_token_created
    ON community_open_data_access_logs (token_id, created_at DESC);
