CREATE TABLE community_digest_publications (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    week_key VARCHAR(10) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    item_count INTEGER NOT NULL,
    unresolvable_item_count INTEGER NOT NULL,
    published_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    body TEXT NOT NULL
);

-- One publication per community per week. Publishing the same week twice would send the same
-- digest to residents twice, so the database refuses it rather than relying on callers to check.
CREATE UNIQUE INDEX idx_digest_publications_community_week
    ON community_digest_publications (community_id, week_key);

COMMENT ON COLUMN community_digest_publications.content_hash IS
    'SHA-256 over the deterministic digest body, so a recipient can verify the digest they received matches what was recorded.';
COMMENT ON COLUMN community_digest_publications.body IS
    'The rendered digest, frozen at publication. Regenerating a past week must reproduce this exactly.';