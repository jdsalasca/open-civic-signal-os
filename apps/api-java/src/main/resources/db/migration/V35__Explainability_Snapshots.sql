CREATE TABLE explainability_snapshots (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    label VARCHAR(160) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    formula_version VARCHAR(20) NOT NULL,
    entry_count INTEGER NOT NULL,
    payload TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_explainability_snapshots_community_created
    ON explainability_snapshots (community_id, created_at DESC);

COMMENT ON COLUMN explainability_snapshots.payload IS
    'Canonical JSON of the frozen ranked rows. Never mutated after insert.';
COMMENT ON COLUMN explainability_snapshots.content_hash IS
    'SHA-256 over the canonical payload. Lets a later reader prove the snapshot was not altered.';