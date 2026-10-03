CREATE TABLE digest_schedule_runs (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    week_key VARCHAR(10) NOT NULL,
    -- What the run did. PREPARED means the digest was generated and left for a person to publish;
    -- SKIPPED means there was nothing to do; FAILED means it could not be generated.
    outcome VARCHAR(20) NOT NULL,
    detail TEXT NULL,
    ran_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_digest_schedule_runs_community_week
    ON digest_schedule_runs (community_id, week_key DESC);

-- One run per community per week. A scheduler that fires twice, or a restart mid-week, must not
-- produce two records for the same week and make it look like two attempts happened.
CREATE UNIQUE INDEX idx_digest_schedule_runs_unique
    ON digest_schedule_runs (community_id, week_key);

COMMENT ON COLUMN digest_schedule_runs.outcome IS
    'PREPARED, SKIPPED or FAILED. There is deliberately no PUBLISHED: the scheduler prepares and a person publishes, so a bulletin cannot reach residents without someone accountable for it.';
COMMENT ON TABLE digest_schedule_runs IS
    'What the weekly digest scheduler did, per community per week. The scheduler does not send: it generates the digest and records that it is ready, and publishing remains a deliberate act.';