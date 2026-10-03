CREATE TABLE formula_change_proposals (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    rationale TEXT NOT NULL,
    proposed_weights TEXT NOT NULL,
    current_weights TEXT NOT NULL,
    impact_preview TEXT NOT NULL,
    sample_size INTEGER NOT NULL,
    positions_moved INTEGER NOT NULL,
    proposed_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    proposed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
    decided_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    decided_at TIMESTAMP NULL,
    decision_note TEXT NULL
);

CREATE INDEX idx_formula_proposals_status_proposed
    ON formula_change_proposals (status, proposed_at DESC);

COMMENT ON COLUMN formula_change_proposals.impact_preview IS
    'The computed effect of the proposal on the real backlog at proposal time, kept verbatim so a decision is judged against the evidence the decider was shown.';
COMMENT ON COLUMN formula_change_proposals.current_weights IS
    'Snapshot of the weights in force when the proposal was made, so a later reader can see what it was changing from.';
COMMENT ON COLUMN formula_change_proposals.sample_size IS
    'How many signals the preview reordered. A preview over three signals proves nothing, so the size travels with the result.';

-- A proposal is versioned evidence, not a live config. There is deliberately no column that the
-- scoring code reads: approving a proposal records a decision, it does not change what runs.
COMMENT ON TABLE formula_change_proposals IS
    'Governance records for proposed scoring changes. Applying an approved proposal is a code change plus an ADR plus a formula version bump, never a row update here.';