CREATE TABLE community_permission_policies (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    scope VARCHAR(80) NOT NULL,
    allowed_roles VARCHAR(400) NOT NULL,
    updated_by UUID NOT NULL REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_community_permission_scope UNIQUE (community_id, scope)
);

CREATE INDEX idx_community_permission_policies_community
    ON community_permission_policies (community_id);
