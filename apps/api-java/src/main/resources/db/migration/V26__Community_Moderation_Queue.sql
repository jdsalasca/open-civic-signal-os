CREATE TABLE community_moderation_reports (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason_code VARCHAR(40) NOT NULL,
    details TEXT NOT NULL,
    target_content_preview TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    content_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    false_positive_review_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by_user_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    resolution_reason TEXT NULL,
    linked_sanction_id UUID NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL
);

CREATE INDEX idx_community_moderation_reports_community_created
    ON community_moderation_reports (community_id, created_at DESC);

CREATE INDEX idx_community_moderation_reports_target
    ON community_moderation_reports (community_id, target_type, target_id);

CREATE TABLE community_sanctions (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_id UUID NULL REFERENCES community_moderation_reports(id) ON DELETE SET NULL,
    sanction_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reason TEXT NOT NULL,
    starts_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP NULL,
    revoked_by_user_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    revoked_reason TEXT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_sanctions_target_status
    ON community_sanctions (community_id, target_user_id, status, created_at DESC);