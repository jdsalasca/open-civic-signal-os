CREATE TABLE participatory_budgets (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    -- Minor units (cents) as a long, never a floating point. Money in a double is a rounding bug
    -- waiting for a council meeting to expose it.
    total_budget_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL
);

CREATE INDEX idx_participatory_budgets_community
    ON participatory_budgets (community_id, created_at DESC);

CREATE TABLE participatory_budget_allocations (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES participatory_budgets(id) ON DELETE CASCADE,
    proposal_id UUID NOT NULL REFERENCES community_proposals(id) ON DELETE CASCADE,
    -- Null when the proposal's estimatedCost could not be read as an amount. Recorded as null rather
    -- than guessed, and surfaced in the simulation as an unreadable cost.
    amount_minor BIGINT NULL,
    raw_cost_text TEXT NOT NULL,
    included BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One allocation per proposal per budget. A proposal counted twice would spend the same money twice.
CREATE UNIQUE INDEX idx_budget_allocations_unique
    ON participatory_budget_allocations (budget_id, proposal_id);

COMMENT ON COLUMN participatory_budget_allocations.amount_minor IS
    'Parsed from the proposal estimatedCost. Null means the text could not be read as an amount, which is reported rather than guessed.';
COMMENT ON COLUMN participatory_budget_allocations.raw_cost_text IS
    'The original text, kept so a reader can see what the parser was given when it failed.';
COMMENT ON TABLE participatory_budgets IS
    'A community budget envelope for a participatory exercise. The platform does not hold money; it records what a community decided to allocate and reports whether the arithmetic fits.';