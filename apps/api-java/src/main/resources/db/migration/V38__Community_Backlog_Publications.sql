CREATE TABLE community_backlog_publications (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL REFERENCES explainability_snapshots(id) ON DELETE CASCADE,
    label VARCHAR(160) NOT NULL,
    ordering_hash VARCHAR(64) NOT NULL,
    formula_version VARCHAR(20) NOT NULL,
    item_count INTEGER NOT NULL,
    published_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Exactly one publication per community can be current. The portable way to enforce that
    -- without a partial index (which H2 does not support and Postgres does) is a nullable slot:
    -- the current row holds 1, superseded rows hold NULL, and NULLs never collide in a unique index
    -- on either engine. So (community_id, current_slot) is unique while allowing any number of
    -- superseded rows.
    current_slot INTEGER NULL
);

CREATE UNIQUE INDEX idx_backlog_publications_current
    ON community_backlog_publications (community_id, current_slot);

CREATE INDEX idx_backlog_publications_community_published
    ON community_backlog_publications (community_id, published_at DESC);

COMMENT ON COLUMN community_backlog_publications.current_slot IS
    '1 when this publication is the live one, NULL when superseded. Guards "which version is live" at the database level rather than by convention.';
COMMENT ON COLUMN community_backlog_publications.ordering_hash IS
    'SHA-256 over the published ranking as sorted id:score lines. Compared against the live ranking to answer whether what a visitor sees is still what was published.';
COMMENT ON COLUMN community_backlog_publications.snapshot_id IS
    'The frozen explainability snapshot this publication is based on, so the ranking itself is recoverable and tamper-evident.';